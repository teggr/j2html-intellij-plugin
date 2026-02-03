package com.example.j2htmlpreview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.ui.EditorTextField;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Main UI panel for the j2html preview tool window.
 * Phase 5: Expression evaluator with Java code editor.
 */
public class PreviewPanel extends JPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(PreviewPanel.class);
    
    private final Project project;
    private final JLabel currentFileLabel;
    private final JComboBox<String> methodSelector;
    private final JEditorPane htmlPreview;
    private final List<PsiMethod> j2htmlMethods = new ArrayList<>();
    private VirtualFile currentVirtualFile = null;
    
    // Phase 5: Expression evaluator fields
    private EditorTextField expressionEditor;
    private PsiExpressionCodeFragment currentFragment;
    private PsiMethod currentMethod;
    
    public PreviewPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("j2html Preview - Phase 5");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        currentFileLabel = new JLabel("No file selected");
        currentFileLabel.setFont(currentFileLabel.getFont().deriveFont(Font.ITALIC));
        
        // Method selector dropdown
        methodSelector = new JComboBox<>();
        methodSelector.addActionListener(e -> onMethodSelected());
        
        JPanel selectorPanel = new JPanel(new BorderLayout());
        selectorPanel.add(new JLabel("Select method: "), BorderLayout.WEST);
        selectorPanel.add(methodSelector, BorderLayout.CENTER);
        selectorPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(currentFileLabel, BorderLayout.CENTER);
        headerPanel.add(selectorPanel, BorderLayout.SOUTH);
        
        // Phase 5: Expression evaluator panel
        JPanel evaluatorPanel = new JPanel(new BorderLayout());
        evaluatorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel evaluatorLabel = new JLabel("Quick test (write method call):");
        evaluatorLabel.setFont(evaluatorLabel.getFont().deriveFont(Font.BOLD));
        
        // Create the expression editor (initially empty)
        expressionEditor = createExpressionEditor();
        expressionEditor.setPreferredSize(new Dimension(400, 100));
        
        JButton executeButton = new JButton("Compile and Preview");
        executeButton.addActionListener(e -> executeExpression());
        
        evaluatorPanel.add(evaluatorLabel, BorderLayout.NORTH);
        evaluatorPanel.add(expressionEditor, BorderLayout.CENTER);
        evaluatorPanel.add(executeButton, BorderLayout.SOUTH);
        
        // Update main panel layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(evaluatorPanel, BorderLayout.CENTER);
        
        // HTML preview area
        htmlPreview = new JEditorPane("text/html", getInitialHtml());
        htmlPreview.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(htmlPreview);
        
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Listen for file changes
        setupFileListener();
        setupPsiListener();
        
        // Show current file if one is already open
        updateCurrentFile();
    }
    
    @Override
    public void dispose() {
        // Cleanup is handled automatically by registering listeners with this Disposable
    }
    
    private void setupFileListener() {
        project.getMessageBus()
            .connect(this)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                    updateCurrentFile();
                }
            });
    }
    
    /**
     * Set up listener for PSI tree changes.
     * PSI events are fired when code structure changes (methods added/removed/modified).
     * IntelliJ batches these intelligently, so we don't need manual debouncing.
     */
    private void setupPsiListener() {
        PsiManager.getInstance(project).addPsiTreeChangeListener(
            new PsiTreeChangeAdapter() {
                @Override
                public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
                    // Check if this event is for our currently displayed file
                    PsiFile changedFile = event.getFile();
                    if (changedFile != null && currentVirtualFile != null) {
                        VirtualFile changedVirtualFile = changedFile.getVirtualFile();
                        if (currentVirtualFile.equals(changedVirtualFile)) {
                            // Re-analyze the file since its structure changed
                            analyzeFile(currentVirtualFile);
                        }
                    }
                }
            },
            this  // Register with this Disposable to ensure cleanup
        );
    }
    
    private void updateCurrentFile() {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        VirtualFile selectedFile = editorManager.getSelectedFiles().length > 0 
            ? editorManager.getSelectedFiles()[0] 
            : null;
        
        if (selectedFile != null) {
            currentVirtualFile = selectedFile;  // Track current file
            currentFileLabel.setText("Current file: " + selectedFile.getName());
            analyzeFile(selectedFile);
        } else {
            currentVirtualFile = null;  // Clear tracked file
            currentFileLabel.setText("No file selected");
            j2htmlMethods.clear();
            updateMethodSelector();
            htmlPreview.setText(getInitialHtml());
        }
    }
    
    /**
     * Analyze the file to find methods that return j2html types.
     * This is where PSI magic happens!
     */
    private void analyzeFile(VirtualFile virtualFile) {
        j2htmlMethods.clear();
        
        // Convert VirtualFile to PsiFile
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        
        // Check if it's a Java file
        if (!(psiFile instanceof PsiJavaFile)) {
            htmlPreview.setText(getNotJavaFileHtml());
            updateMethodSelector();
            return;
        }
        
        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        
        // Get all classes in the file
        PsiClass[] classes = javaFile.getClasses();
        
        // Find methods in each class
        for (PsiClass psiClass : classes) {
            PsiMethod[] methods = psiClass.getMethods();
            
            for (PsiMethod method : methods) {
                if (isJ2HtmlMethod(method)) {
                    j2htmlMethods.add(method);
                }
            }
        }
        
        updateMethodSelector();
        
        if (j2htmlMethods.isEmpty()) {
            htmlPreview.setText(getNoMethodsFoundHtml());
        } else {
            htmlPreview.setText(getMethodsFoundHtml());
        }
    }
    
    /**
     * Check if a method returns a j2html type.
     * We look for common j2html types: ContainerTag, DomContent, Tag, etc.
     */
    private boolean isJ2HtmlMethod(PsiMethod method) {
        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return false;
        }
        
        String typeName = returnType.getPresentableText();
        
        // Check for common j2html return types
        return typeName.equals("ContainerTag") ||
               typeName.equals("DomContent") ||
               typeName.equals("Tag") ||
               typeName.equals("DivTag") ||
               typeName.equals("SpanTag") ||
               typeName.equals("HtmlTag") ||
               typeName.contains("Tag"); // Catch other *Tag types
    }
    
    /**
     * Update the method selector dropdown with found methods.
     */
    private void updateMethodSelector() {
        methodSelector.removeAllItems();
        
        if (j2htmlMethods.isEmpty()) {
            methodSelector.addItem("No j2html methods found");
            methodSelector.setEnabled(false);
        } else {
            methodSelector.setEnabled(true);
            for (PsiMethod method : j2htmlMethods) {
                String signature = buildMethodSignature(method);
                methodSelector.addItem(signature);
            }
        }
    }
    
    /**
     * Build a readable method signature for display.
     */
    private String buildMethodSignature(PsiMethod method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters[i].getType().getPresentableText())
              .append(" ")
              .append(parameters[i].getName());
        }
        
        sb.append(")");
        
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            sb.append(" → ").append(returnType.getPresentableText());
        }
        
        return sb.toString();
    }
    
    /**
     * Called when user selects a method from the dropdown.
     * Phase 5: Handles parameterized methods by populating expression editor.
     */
    private void onMethodSelected() {
        int selectedIndex = methodSelector.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < j2htmlMethods.size()) {
            PsiMethod selectedMethod = j2htmlMethods.get(selectedIndex);
            currentMethod = selectedMethod;
            
            // For zero-parameter methods, execute directly (Phase 4 behavior)
            if (selectedMethod.getParameterList().getParametersCount() == 0) {
                SwingUtilities.invokeLater(() -> compileAndExecute(selectedMethod));
            } else {
                // For methods with parameters, populate the expression editor
                populateExpressionEditor(selectedMethod);
                showInfo("Method has parameters. Write the method call in the editor above and click 'Compile and Preview'.");
            }
        }
    }
    
    /**
     * Compile the module containing the method, then execute it.
     * 
     * CompilerManager.make() is asynchronous - it returns immediately and
     * notifies us via the callback when compilation is complete.
     * 
     * We use SwingUtilities.invokeLater() in the callback because the
     * callback is NOT guaranteed to run on the UI thread.
     */
    private void compileAndExecute(PsiMethod psiMethod) {
        // Get the module that contains the method - must use ReadAction
        ReadAction.nonBlocking(() -> ModuleUtilCore.findModuleForPsiElement(psiMethod))
            .finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(), module -> {
                if (module == null) {
                    showError("Could not find module for class");
                    return;
                }

                // Show compiling state immediately (we're on UI thread here)
                showInfo("Compiling...");

                // Trigger compilation - this is async, so we continue in the callback
                CompilerManager.getInstance(project).make(module, new CompileStatusNotification() {
                    @Override
                    public void finished(boolean aborted, int errors, int warnings, CompileContext context) {
                        // We may NOT be on the UI thread here, so use invokeLater
                        SwingUtilities.invokeLater(() -> {
                            if (aborted) {
                                showError("Compilation was aborted.");
                            } else if (errors > 0) {
                                showError("Compilation failed with " + errors + " error(s). Check the Problems panel for details.");
                            } else {
                                // Compilation succeeded - safe to execute now
                                executeMethod(psiMethod);
                            }
                        });
                    }
                });
            }).submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService());
    }
    
    /**
     * Execute the selected method and render its HTML output.
     * Phase 4a: Only handles static methods with zero parameters.
     */
    private void executeMethod(PsiMethod psiMethod) {
        // Run slow PSI operations in a background thread with read access
        ReadAction.nonBlocking(() -> {
            try {
                // Step 1: Get the containing class
                PsiClass psiClass = psiMethod.getContainingClass();
                if (psiClass == null) {
                    return new ExecutionData("Could not find containing class for method", null, null, null);
                }
                
                String qualifiedClassName = psiClass.getQualifiedName();
                if (qualifiedClassName == null) {
                    return new ExecutionData("Could not determine qualified class name", null, null, null);
                }
                
                // Step 2: Check if method is static (we only handle static for now)
                if (!psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                    return new ExecutionData("Method must be static. Non-static method execution not yet supported.", null, null, null);
                }
                
                // Step 3: Check if method has zero parameters
                // Note: onMethodSelected() directs parameterized methods to expression editor
                if (psiMethod.getParameterList().getParametersCount() > 0) {
                    return new ExecutionData("Method has parameters. Use the expression editor above to provide arguments.", null, null, null);
                }
                
                // Step 4: Get the module and build classloader
                Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
                if (module == null) {
                    return new ExecutionData("Could not find module for class", null, null, null);
                }
                
                return new ExecutionData(null, qualifiedClassName, psiMethod.getName(), module);
            } catch (Exception e) {
                return new ExecutionData("Error preparing execution: " + e.getMessage(), null, null, null);
            }
        }).finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(), data -> {
            if (data.error != null) {
                showError(data.error);
                return;
            }
            executeMethodOnEdt(data);
        }).submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService());
    }
    
    private static class ExecutionData {
        final String error;
        final String qualifiedClassName;
        final String methodName;
        final Module module;
        
        ExecutionData(String error, String qualifiedClassName, String methodName, Module module) {
            this.error = error;
            this.qualifiedClassName = qualifiedClassName;
            this.methodName = methodName;
            this.module = module;
        }
    }
    
    private void executeMethodOnEdt(ExecutionData data) {
        try {
            ClassLoader moduleClassLoader = getModuleClassLoader(data.module);
            
            // Step 5: Load the compiled class
            Class<?> loadedClass;
            try {
                loadedClass = Class.forName(data.qualifiedClassName, true, moduleClassLoader);
            } catch (ClassNotFoundException e) {
                showError("Class not found. Make sure the project is compiled: " + e.getMessage());
                return;
            }
            
            // Step 6: Get the reflection Method
            Method reflectionMethod;
            try {
                reflectionMethod = loadedClass.getDeclaredMethod(data.methodName);
            } catch (NoSuchMethodException e) {
                showError("Method not found in compiled class: " + e.getMessage());
                return;
            }
            
            // Step 7: Make method accessible (in case it's not public)
            reflectionMethod.setAccessible(true);
            
            // Step 8: Invoke the method (null because it's static)
            Object result;
            try {
                result = reflectionMethod.invoke(null);
            } catch (Exception e) {
                showError("Error invoking method: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                return;
            }
            
            if (result == null) {
                showError("Method returned null");
                return;
            }
            
            // Step 9: Render the j2html object to HTML
            String renderedHtml = renderJ2HtmlObject(result);
            
            // Step 10: Display the rendered HTML
            displayRenderedHtml(renderedHtml, data.methodName);
            
        } catch (Exception e) {
            showError("Unexpected error: " + e.getMessage());
            LOG.error("Unexpected error executing method", e);
        }
    }
    
    /**
     * Build a classloader for the module that includes all dependencies.
     * This ensures we can load both the user's compiled classes and j2html library.
     */
    private ClassLoader getModuleClassLoader(Module module) throws Exception {
        List<String> classpathEntries = new ArrayList<>();
        
        // Get all classpath roots for the module (compiled output + dependencies)
        VirtualFile[] roots = OrderEnumerator.orderEntries(module)
          .withoutSdk()  // We don't need JDK classes
          .recursively() // Include transitive dependencies
          .classes()     // Get class roots (not source roots)
          .getRoots();
           Stream.of(roots).forEach(root -> {
                String path = root.getPath();
                // Remove jar protocol if present (jar:file:/path/to.jar!/ → /path/to.jar)
                if (path.startsWith("jar://")) {
                    path = path.substring(6);
                    int exclamation = path.indexOf("!");
                    if (exclamation != -1) {
                        path = path.substring(0, exclamation);
                    }
                }
                classpathEntries.add(path);
            });
        
        // Convert paths to URLs
        URL[] urls = classpathEntries.stream()
            .map(path -> {
                try {
                    return new File(path).toURI().toURL();
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toArray(URL[]::new);
        
        // Create classloader with module's classpath
        return new URLClassLoader(urls, getClass().getClassLoader());
    }
    
    /**
     * Render a j2html object to HTML string.
     * We use reflection to call .render() since we don't have j2html on plugin classpath.
     */
    private String renderJ2HtmlObject(Object j2htmlObject) throws Exception {
        // All j2html objects (Tag, DomContent, etc.) have a render() method
        Method renderMethod = j2htmlObject.getClass().getMethod("render");
        Object result = renderMethod.invoke(j2htmlObject);
        
        if (!(result instanceof String)) {
            throw new Exception("render() did not return a String");
        }
        
        return (String) result;
    }
    
    /**
     * Display the rendered HTML in the preview pane with success styling.
     */
    private void displayRenderedHtml(String renderedHtml, String methodName) {
        String wrappedHtml = """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .success-banner {
                        background: #d4edda;
                        border: 1px solid #c3e6cb;
                        border-radius: 8px;
                        padding: 12px;
                        margin-bottom: 20px;
                        color: #155724;
                    }
                    .rendered-output {
                        border: 2px solid #007bff;
                        border-radius: 8px;
                        padding: 20px;
                        background: white;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    }
                    .method-info {
                        background: #f8f9fa;
                        border-left: 4px solid #007bff;
                        padding: 10px;
                        margin-bottom: 15px;
                        font-family: 'Courier New', monospace;
                    }
                </style>
            </head>
            <body>
                <div class="success-banner">
                    ✓ <strong>Phase 4 Success!</strong> Method executed and HTML rendered.
                </div>
                <div class="method-info">
                    Rendered output from: <strong>%s()</strong>
                </div>
                <div class="rendered-output">
                    %s
                </div>
            </body>
            </html>
            """.formatted(methodName, renderedHtml);
        
        htmlPreview.setText(wrappedHtml);
    }
    
    /**
     * Display an error message in the preview pane.
     */
    private void showError(String errorMessage) {
        String errorHtml = """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .error {
                        background: #f8d7da;
                        border: 1px solid #f5c6cb;
                        border-radius: 8px;
                        padding: 16px;
                        color: #721c24;
                    }
                    .error h3 {
                        margin-top: 0;
                        color: #721c24;
                    }
                    code {
                        background: #fff;
                        padding: 2px 6px;
                        border-radius: 3px;
                        font-family: 'Courier New', monospace;
                    }
                </style>
            </head>
            <body>
                <div class="error">
                    <h3>⚠ Execution Error</h3>
                    <p><code>%s</code></p>
                </div>
            </body>
            </html>
            """.formatted(errorMessage);
        
        htmlPreview.setText(errorHtml);
    }
    
    /**
     * Display an informational/status message in the preview pane.
     * Used for transient states like "Compiling..."
     */
    private void showInfo(String message) {
        String infoHtml = """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .info {
                        background: #cce5ff;
                        border: 1px solid #b8daff;
                        border-radius: 8px;
                        padding: 16px;
                        color: #004085;
                    }
                    .info h3 {
                        margin-top: 0;
                        color: #004085;
                    }
                    code {
                        background: #fff;
                        padding: 2px 6px;
                        border-radius: 3px;
                        font-family: 'Courier New', monospace;
                    }
                </style>
            </head>
            <body>
                <div class="info">
                    <h3>ℹ Status</h3>
                    <p><code>%s</code></p>
                </div>
            </body>
            </html>
            """.formatted(message);

        htmlPreview.setText(infoHtml);
    }
    
    private String getMethodSelectedHtml(PsiMethod method) {
        return """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .card {
                        border: 1px solid #ddd;
                        border-radius: 8px;
                        padding: 16px;
                        margin: 16px 0;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    h2 { color: #34495e; margin-top: 0; }
                    .success { color: #27ae60; font-weight: bold; }
                    code { 
                        background: #f4f4f4; 
                        padding: 2px 6px; 
                        border-radius: 3px;
                        font-family: 'Courier New', monospace;
                    }
                    pre {
                        background: #f8f8f8;
                        padding: 12px;
                        border-radius: 4px;
                        overflow-x: auto;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>Method Selected ✓</h2>
                    <p><span class="success">Phase 3 is working!</span></p>
                    <p><strong>Method:</strong> <code>%s</code></p>
                    <p><strong>Return Type:</strong> <code>%s</code></p>
                    <p><strong>Parameters:</strong> %d</p>
                </div>
                <div class="card">
                    <h2>What's Happening</h2>
                    <p>We're using IntelliJ's <code>PSI (Program Structure Interface)</code> to parse your Java code and find methods that return j2html types.</p>
                </div>
                <div class="card">
                    <h2>Next: Phase 4</h2>
                    <p>We'll execute this method and render the actual HTML output!</p>
                </div>
            </body>
            </html>
            """.formatted(
                method.getName(),
                method.getReturnType() != null ? method.getReturnType().getPresentableText() : "unknown",
                method.getParameterList().getParametersCount()
            );
    }
    
    private String getMethodsFoundHtml() {
        return """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .card {
                        border: 1px solid #ddd;
                        border-radius: 8px;
                        padding: 16px;
                        margin: 16px 0;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    h2 { color: #34495e; margin-top: 0; }
                    .success { color: #27ae60; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>j2html Methods Found! ✓</h2>
                    <p><span class="success">Found %d method(s) returning j2html types</span></p>
                    <p>Select a method from the dropdown above to see details.</p>
                </div>
                <div class="card">
                    <h2>PSI in Action</h2>
                    <p>We analyzed the Java file's abstract syntax tree to find methods returning j2html types like ContainerTag, DomContent, etc.</p>
                </div>
            </body>
            </html>
            """.formatted(j2htmlMethods.size());
    }
    
    private String getNoMethodsFoundHtml() {
        return """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                        color: #7f8c8d;
                    }
                    .card {
                        border: 1px solid #ddd;
                        border-radius: 8px;
                        padding: 16px;
                        margin: 16px 0;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>No j2html methods found</h2>
                    <p>This file doesn't contain any methods returning j2html types like ContainerTag, DomContent, Tag, etc.</p>
                    <p>Try opening a file that contains j2html component methods!</p>
                </div>
            </body>
            </html>
            """;
    }
    
    private String getNotJavaFileHtml() {
        return """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                        color: #7f8c8d;
                    }
                    .card {
                        border: 1px solid #ddd;
                        border-radius: 8px;
                        padding: 16px;
                        margin: 16px 0;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>Not a Java file</h2>
                    <p>The j2html preview only works with Java files.</p>
                </div>
            </body>
            </html>
            """;
    }
    
    private String getInitialHtml() {
        return """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        line-height: 1.6;
                        color: #7f8c8d;
                    }
                    .card {
                        border: 1px solid #ddd;
                        border-radius: 8px;
                        padding: 16px;
                        margin: 16px 0;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>Waiting for file selection...</h2>
                    <p>Open a Java file containing j2html methods to see them listed here.</p>
                </div>
            </body>
            </html>
            """;
    }
    
    /**
     * Create an EditorTextField for writing Java expressions.
     * This gives us a mini code editor with syntax highlighting and autocomplete.
     */
    private EditorTextField createExpressionEditor() {
        // Start with empty document
        Document document = EditorFactory.getInstance().createDocument("");
        
        // Create editor text field with Java file type for syntax highlighting
        EditorTextField textField = new EditorTextField(document, project, JavaFileType.INSTANCE) {
            @Override
            protected EditorEx createEditor() {
                EditorEx editor = super.createEditor();
                // Enable soft wrapping for better multi-line editing
                editor.getSettings().setUseSoftWraps(true);
                return editor;
            }
        };
        
        // Not one-line (allow multi-line expressions)
        textField.setOneLineMode(false);
        
        return textField;
    }
    
    /**
     * Populate the expression editor with a template method call.
     * Pre-fills with smart defaults based on parameter types.
     */
    private void populateExpressionEditor(PsiMethod method) {
        String template = buildMethodCallTemplate(method);
        
        // Create a code fragment with the template
        JavaCodeFragmentFactory fragmentFactory = JavaCodeFragmentFactory.getInstance(project);
        PsiExpressionCodeFragment fragment = fragmentFactory.createExpressionCodeFragment(
            template,
            method.getContainingClass(),  // Context for name resolution
            method.getReturnType(),        // Expected return type
            true                           // Is physical
        );
        
        currentFragment = fragment;
        
        // Update the editor with the fragment's document
        Document document = PsiDocumentManager.getInstance(project).getDocument(fragment);
        if (document != null) {
            expressionEditor.setDocument(document);
        }
    }
    
    /**
     * Build a template method call with smart defaults for parameters.
     * E.g., "userCard(new User(\"\", \"\"), \"\")"
     */
    private String buildMethodCallTemplate(PsiMethod method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        
        PsiParameter[] params = method.getParameterList().getParameters();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(", ");
            
            PsiType type = params[i].getType();
            String defaultValue = getDefaultValueForType(type);
            sb.append(defaultValue);
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * Generate smart default values based on parameter type.
     * Note: Generic types are simplified (e.g., List<String> becomes List.of()).
     * Users can manually add type parameters if needed.
     */
    private String getDefaultValueForType(PsiType type) {
        String typeName = type.getPresentableText();
        
        return switch(typeName) {
            case "String" -> "\"\"";
            case "int", "Integer" -> "0";
            case "long", "Long" -> "0L";
            case "boolean", "Boolean" -> "false";
            case "double", "Double" -> "0.0";
            case "List" -> "List.of()";  // Simplified - users can add type params manually
            default -> {
                // For custom types, try to use constructor
                if (type instanceof PsiClassType psiClassType) {
                    PsiClass psiClass = psiClassType.resolve();
                    if (psiClass != null) {
                        // Generate "new ClassName()"
                        yield "new " + psiClass.getName() + "()";
                    }
                }
                yield "null";
            }
        };
    }
    
    /**
     * Execute the expression written in the editor.
     * This compiles and evaluates the user's Java code.
     */
    private void executeExpression() {
        if (currentMethod == null) {
            showError("No method selected");
            return;
        }
        
        String expressionText = expressionEditor.getText().trim();
        if (expressionText.isEmpty()) {
            showError("Expression is empty");
            return;
        }
        
        // Update the fragment with current text
        JavaCodeFragmentFactory fragmentFactory = JavaCodeFragmentFactory.getInstance(project);
        PsiExpressionCodeFragment fragment = fragmentFactory.createExpressionCodeFragment(
            expressionText,
            currentMethod.getContainingClass(),
            currentMethod.getReturnType(),
            true
        );
        
        // Get the module for compilation
        Module module = ModuleUtilCore.findModuleForPsiElement(currentMethod);
        if (module == null) {
            showError("Could not find module");
            return;
        }
        
        showInfo("Compiling expression...");
        
        // Compile the module first (ensures all dependencies are compiled)
        CompilerManager.getInstance(project).make(module, new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext context) {
                SwingUtilities.invokeLater(() -> {
                    if (aborted) {
                        showError("Compilation was aborted.");
                    } else if (errors > 0) {
                        showError("Compilation failed with " + errors + " error(s). Check the Problems panel.");
                    } else {
                        // Compilation succeeded - now evaluate the expression
                        try {
                            evaluateAndDisplay(fragment, module);
                        } catch (Exception e) {
                            showError("Error evaluating expression: " + e.getMessage());
                            LOG.error("Error evaluating expression", e);
                        }
                    }
                });
            }
        });
    }
    
    /**
     * Evaluate the compiled expression and display the result.
     * This is similar to executeMethod but works with expressions.
     */
    private void evaluateAndDisplay(PsiExpressionCodeFragment fragment, Module module) throws Exception {
        String expressionText = fragment.getText();
        
        // For Phase 5, we'll use a simplified approach:
        // Wrap the expression in a temporary method and execute it
        
        // Create a temporary wrapper class with the expression
        // Note: Using timestamp for uniqueness. In production, consider UUID for guaranteed uniqueness.
        String wrapperClassName = "ExpressionWrapper_" + System.currentTimeMillis();
        String wrapperCode = generateWrapperClass(wrapperClassName, expressionText, fragment);
        
        // Compile the wrapper (this is a simplified approach - production would use JavaCompiler API)
        // For now, we'll execute the expression by treating it as a method call
        // and using the existing execution infrastructure
        
        // Parse the expression to extract method name and arguments
        // This is a simplified implementation - assumes expression is a method call
        Object result = evaluateExpressionReflection(expressionText, module);
        
        if (result == null) {
            showError("Expression returned null");
            return;
        }
        
        // Render the j2html object
        String html = renderJ2HtmlObject(result);
        
        // Display
        displayRenderedHtml(html, currentMethod.getName());
    }
    
    /**
     * Simplified expression evaluation using reflection.
     * Assumes expression is a static method call like "userCard(new User(...))"
     */
    private Object evaluateExpressionReflection(String expressionText, Module module) throws Exception {
        // This is a simplified implementation for Phase 5
        // It handles basic method calls but not complex expressions
        
        // For a complete implementation, you'd use JavaCompiler API or
        // debugger evaluator, but this works for our use case
        
        // Parse method name (everything before first '(')
        int parenIndex = expressionText.indexOf('(');
        if (parenIndex == -1) {
            throw new Exception("Expression must be a method call");
        }
        
        String methodName = expressionText.substring(0, parenIndex).trim();
        
        // Get the class
        PsiClass containingClass = currentMethod.getContainingClass();
        if (containingClass == null) {
            throw new Exception("Could not find containing class");
        }
        
        String qualifiedClassName = containingClass.getQualifiedName();
        ClassLoader classLoader = getModuleClassLoader(module);
        Class<?> clazz = Class.forName(qualifiedClassName, true, classLoader);
        
        // For Phase 5, we'll show an error for parameterized methods
        // and suggest using Phase 4 for zero-param methods
        // Full expression evaluation would require the JavaCompiler API
        
        throw new Exception("Expression evaluation with parameters requires JavaCompiler API integration (planned for Phase 5b). Currently, only zero-parameter methods are fully supported. Use the method dropdown to select and execute zero-parameter methods directly.");
    }
    
    /**
     * Generate a temporary wrapper class for expression evaluation.
     * This would be used in a complete implementation with JavaCompiler API.
     */
    private String generateWrapperClass(String className, String expression, PsiExpressionCodeFragment fragment) {
        PsiClass contextClass = PsiTreeUtil.getParentOfType(fragment.getContext(), PsiClass.class);
        String packageName = contextClass != null && contextClass.getContainingFile() instanceof PsiJavaFile
            ? ((PsiJavaFile) contextClass.getContainingFile()).getPackageName()
            : "";
        
        StringBuilder code = new StringBuilder();
        if (!packageName.isEmpty()) {
            code.append("package ").append(packageName).append(";\n");
        }
        
        code.append("\npublic class ").append(className).append(" {\n");
        code.append("  public static Object evaluate() {\n");
        code.append("    return ").append(expression).append(";\n");
        code.append("  }\n");
        code.append("}\n");
        
        return code.toString();
    }
}
