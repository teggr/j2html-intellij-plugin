package com.example.j2htmlpreview;

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

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Main UI panel for the j2html preview tool window.
 * Phase 4: Executes methods and renders HTML output.
 */
public class PreviewPanel extends JPanel {
    private final Project project;
    private final JLabel currentFileLabel;
    private final JComboBox<String> methodSelector;
    private final JEditorPane htmlPreview;
    private final List<PsiMethod> j2htmlMethods = new ArrayList<>();
    private VirtualFile currentVirtualFile = null;
    
    public PreviewPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("j2html Preview - Phase 4");
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
        
        // HTML preview area
        htmlPreview = new JEditorPane("text/html", getInitialHtml());
        htmlPreview.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(htmlPreview);
        
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Listen for file changes
        setupFileListener();
        setupPsiListener();  // ADD THIS LINE
        
        // Show current file if one is already open
        updateCurrentFile();
    }
    
    private void setupFileListener() {
        project.getMessageBus()
            .connect()
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
            // This disposable ensures the listener is cleaned up when the tool window closes
            // For now, we'll use a simple approach - in production you'd want proper disposal
            () -> {}
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
     * Now executes the method and renders its output!
     */
    private void onMethodSelected() {
        int selectedIndex = methodSelector.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < j2htmlMethods.size()) {
            PsiMethod selectedMethod = j2htmlMethods.get(selectedIndex);
            
            // Execute the method and render its HTML
            executeMethod(selectedMethod);
        }
    }
    
    /**
     * Execute the selected method and render its HTML output.
     * Phase 4a: Only handles static methods with zero parameters.
     */
    private void executeMethod(PsiMethod psiMethod) {
        try {
            // Step 1: Get the containing class
            PsiClass psiClass = psiMethod.getContainingClass();
            if (psiClass == null) {
                showError("Could not find containing class for method");
                return;
            }
            
            String qualifiedClassName = psiClass.getQualifiedName();
            if (qualifiedClassName == null) {
                showError("Could not determine qualified class name");
                return;
            }
            
            // Step 2: Check if method is static (we only handle static for now)
            if (!psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                showError("Method must be static. Non-static method execution not yet supported.");
                return;
            }
            
            // Step 3: Check if method has zero parameters (Phase 4a limitation)
            if (psiMethod.getParameterList().getParametersCount() > 0) {
                showError("Method has parameters. Parameter handling will be added in Phase 5.");
                return;
            }
            
            // Step 4: Get the module and build classloader
            Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
            if (module == null) {
                showError("Could not find module for class");
                return;
            }
            
            ClassLoader moduleClassLoader = getModuleClassLoader(module);
            
            // Step 5: Load the compiled class
            Class<?> loadedClass;
            try {
                loadedClass = Class.forName(qualifiedClassName, true, moduleClassLoader);
            } catch (ClassNotFoundException e) {
                showError("Class not found. Make sure the project is compiled: " + e.getMessage());
                return;
            }
            
            // Step 6: Get the reflection Method
            Method reflectionMethod;
            try {
                reflectionMethod = loadedClass.getDeclaredMethod(psiMethod.getName());
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
            displayRenderedHtml(renderedHtml, psiMethod);
            
        } catch (Exception e) {
            showError("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Build a classloader for the module that includes all dependencies.
     * This ensures we can load both the user's compiled classes and j2html library.
     */
    private ClassLoader getModuleClassLoader(Module module) throws Exception {
        List<String> classpathEntries = new ArrayList<>();
        
        // Get all classpath roots for the module (compiled output + dependencies)
        OrderEnumerator.orderEntries(module)
            .withoutSdk()  // We don't need JDK classes
            .recursively() // Include transitive dependencies
            .classes()     // Get class roots (not source roots)
            .getRoots()
            .forEach(root -> {
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
    private void displayRenderedHtml(String renderedHtml, PsiMethod method) {
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
            """.formatted(method.getName(), renderedHtml);
        
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
}
