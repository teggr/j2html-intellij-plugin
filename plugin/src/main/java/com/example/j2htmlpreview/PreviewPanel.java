package com.example.j2htmlpreview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.ui.EditorTextField;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Alarm;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;

import javax.swing.*;
import javax.tools.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Main UI panel for the j2html preview tool window.
 * Phase 5: Expression evaluator with Java code editor.
 */
public class PreviewPanel extends JPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(PreviewPanel.class);
    private static final int DEBOUNCE_DELAY_MS = 400; // Debounce delay for PSI changes
    private static final long COMPILATION_THROTTLE_MS = 2500; // Minimum interval between compilations
    
    private final Project project;
    private final JComboBox<String> methodSelector;
    private JBCefBrowser webViewComponent;
    private JEditorPane legacyHtmlPane;
    private boolean hasModernBrowserSupport;
    private final List<PsiMethod> j2htmlMethods = new ArrayList<>();
    private VirtualFile currentVirtualFile = null;
    
    // Phase 5: Expression evaluator fields
    private EditorTextField expressionEditor;
    private PsiExpressionCodeFragment currentFragment;
    private PsiMethod currentMethod;
    private JPanel evaluatorPanel;
    
    // Fix for multiple recompilations issue
    private boolean isUpdatingMethodSelector = false; // Prevents ActionListener during programmatic updates
    private final Alarm psiChangeAlarm; // Debounces PSI change events
    private long lastCompilationTime = 0; // Tracks last compilation to throttle
    
    public PreviewPanel(Project project) {
        this.project = project;
        this.psiChangeAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
        setLayout(new BorderLayout());
        
        // Header panel - just the method selector
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Method selector dropdown
        methodSelector = new JComboBox<>();
        methodSelector.addActionListener(e -> {
            // Only fire if not updating programmatically
            if (!isUpdatingMethodSelector) {
                onMethodSelected();
            }
        });
        
        JPanel selectorPanel = new JPanel(new BorderLayout(5, 0));
        selectorPanel.add(new JLabel("Select method:"), BorderLayout.WEST);
        selectorPanel.add(methodSelector, BorderLayout.CENTER);
        
        headerPanel.add(selectorPanel, BorderLayout.NORTH);
        
        // Expression evaluator panel - compact single line
        evaluatorPanel = new JPanel(new BorderLayout(5, 0));
        evaluatorPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Create single-line expression editor
        expressionEditor = createExpressionEditor();
        
        // Create compact execute button with play icon
        JButton executeButton = new JButton("▶");
        executeButton.setToolTipText("Compile and Preview");
        executeButton.getAccessibleContext().setAccessibleDescription("Compile and preview the j2html expression");
        executeButton.setPreferredSize(new Dimension(45, 25));
        executeButton.addActionListener(e -> executeExpression());
        
        // Create "Save as @Preview" button
        JButton saveAsPreviewButton = new JButton("💾");
        saveAsPreviewButton.setToolTipText("Save as @Preview");
        saveAsPreviewButton.getAccessibleContext().setAccessibleDescription("Generate a @Preview annotated method from this expression");
        saveAsPreviewButton.setPreferredSize(new Dimension(45, 25));
        saveAsPreviewButton.addActionListener(e -> saveAsPreview());
        
        // Create button panel to hold both buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(executeButton);
        buttonPanel.add(saveAsPreviewButton);
        
        evaluatorPanel.add(expressionEditor, BorderLayout.CENTER);
        evaluatorPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Initially hidden - only show for parameterized methods
        evaluatorPanel.setVisible(false);
        
        // Assemble main layout
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(headerPanel);
        topPanel.add(evaluatorPanel);
        
        // Detect modern browser availability
        hasModernBrowserSupport = JBCefApp.isSupported();
        JComponent displayArea;
        
        if (hasModernBrowserSupport) {
            webViewComponent = new JBCefBrowser();
            String welcomePage = constructBootstrapPage(getInitialHtml());
            webViewComponent.loadHTML(welcomePage);
            displayArea = webViewComponent.getComponent();

            // Set initial size for the browser component
            displayArea.setPreferredSize(new Dimension(800, 600));

            // Handle resize events to update browser viewport
            // JCEF requires the Canvas component to be properly sized for correct rendering
            displayArea.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    // Force the browser component to update its size
                    Component component = e.getComponent();
                    if (component != null && webViewComponent != null) {
                        // Ensure the browser knows about the new size
                        Dimension size = component.getSize();
                        component.setSize(size);
                        component.revalidate();
                        component.repaint();
                    }
                }
            });
        } else {
            String unavailableNote = "<html><body style='padding:15px;font-family:Arial;'>" +
                                 "<h2 style='color:#d9534f;'>Browser Not Available</h2>" +
                                 "<p>JCEF (Chromium Embedded Framework) is not supported.</p>" +
                                 "<p>Bootstrap features will be unavailable in this session.</p></body></html>";
            legacyHtmlPane = new JEditorPane("text/html", unavailableNote);
            legacyHtmlPane.setEditable(false);
            displayArea = legacyHtmlPane;
        }
        
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setPreferredSize(new Dimension(800, 600)); // Set initial preferred size

        // Handle scroll pane resize to update browser viewport
        if (hasModernBrowserSupport) {
            final JComponent finalDisplayArea = displayArea;
            scrollPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    // Update the display area size when scroll pane is resized
                    if (finalDisplayArea != null && webViewComponent != null) {
                        SwingUtilities.invokeLater(() -> {
                            Dimension viewportSize = scrollPane.getViewport().getExtentSize();
                            finalDisplayArea.setPreferredSize(viewportSize);
                            finalDisplayArea.setSize(viewportSize);
                            finalDisplayArea.revalidate();
                            finalDisplayArea.repaint();
                        });
                    }
                }
            });
        }

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
        psiChangeAlarm.cancelAllRequests();
        if (webViewComponent != null) {
            webViewComponent.dispose();
        }
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
     * Set up listener for PSI tree changes with debouncing.
     * PSI events are fired when code structure changes (methods added/removed/modified).
     * We debounce these events to avoid triggering analysis on every keystroke.
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
                            // Debounce: Cancel any pending request and schedule a new one
                            psiChangeAlarm.cancelAllRequests();
                            psiChangeAlarm.addRequest(() -> analyzeFile(currentVirtualFile), DEBOUNCE_DELAY_MS);
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
            currentVirtualFile = selectedFile;
            analyzeFile(selectedFile);
        } else {
            currentVirtualFile = null;
            j2htmlMethods.clear();
            updateMethodSelector();
            renderToView(getInitialHtml());
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
            renderToView(getNotJavaFileHtml());
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
            renderToView(getNoMethodsFoundHtml());
        } else {
            renderToView(getMethodsFoundHtml());
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
     * Preserves the current selection to avoid unwanted re-execution.
     */
    private void updateMethodSelector() {
        // Prevent ActionListener from firing during programmatic updates
        isUpdatingMethodSelector = true;
        
        try {
            // Store current selection
            int previousIndex = methodSelector.getSelectedIndex();
            String previousSelection = previousIndex >= 0 ? (String) methodSelector.getSelectedItem() : null;
            
            methodSelector.removeAllItems();
            
            if (j2htmlMethods.isEmpty()) {
                methodSelector.addItem("No j2html methods found");
                methodSelector.setEnabled(false);
            } else {
                methodSelector.setEnabled(true);
                
                // Build list of new signatures
                List<String> newSignatures = new ArrayList<>();
                for (PsiMethod method : j2htmlMethods) {
                    String signature = buildMethodSignature(method);
                    newSignatures.add(signature);
                    methodSelector.addItem(signature);
                }
                
                // Restore previous selection if it still exists
                if (previousSelection != null) {
                    int newIndex = newSignatures.indexOf(previousSelection);
                    if (newIndex >= 0) {
                        methodSelector.setSelectedIndex(newIndex);
                    }
                }
            }
        } finally {
            // Re-enable ActionListener
            isUpdatingMethodSelector = false;
        }
    }
    
    /**
     * Build a readable method signature for display.
     * If method has @Preview annotation, use the friendly name instead.
     */
    private String buildMethodSignature(PsiMethod method) {
        // Check if method has @Preview annotation
        PsiAnnotation previewAnnotation = method.getAnnotation("com.example.j2htmlpreview.Preview");
        
        if (previewAnnotation != null) {
            // Extract the name attribute value
            PsiAnnotationMemberValue nameValue = previewAnnotation.findAttributeValue("name");
            if (nameValue != null) {
                // Remove quotes from string literal
                String friendlyName = nameValue.getText().replaceAll("^\"|\"$", "");
                // Only use friendly name if it's not empty
                if (!friendlyName.isEmpty()) {
                    return friendlyName;
                }
            }
        }
        
        // Fallback to standard method signature
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
     */
    private void onMethodSelected() {
        int selectedIndex = methodSelector.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < j2htmlMethods.size()) {
            PsiMethod selectedMethod = j2htmlMethods.get(selectedIndex);
            currentMethod = selectedMethod;
            
            if (selectedMethod.getParameterList().getParametersCount() == 0) {
                // Zero parameters - execute immediately, hide editor
                evaluatorPanel.setVisible(false);
                compileAndExecute(selectedMethod);
            } else {
                // Has parameters - show editor with template and display helpful message
                evaluatorPanel.setVisible(true);
                populateExpressionEditor(selectedMethod);
                // Show message indicating arguments need to be set
                showMethodRequiresArgumentsMessage();
            }
        }
    }
    
    /**
     * Compile the module containing the method, then execute it.
     * Now includes throttling to prevent multiple overlapping compilations.
     * 
     * CompilerManager.make() is asynchronous - it returns immediately and
     * notifies us via the callback when compilation is complete.
     * 
     * We use SwingUtilities.invokeLater() in the callback because the
     * callback is NOT guaranteed to run on the UI thread.
     */
    private void compileAndExecute(PsiMethod psiMethod) {
        // Throttle compilation requests
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCompilationTime < COMPILATION_THROTTLE_MS) {
            showInfo("Please wait... compilation is still in progress or too soon since last compilation.");
            return;
        }
        lastCompilationTime = currentTime;
        
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
    
    private void displayRenderedHtml(String renderedHtml, String methodName) {
        renderToView(renderedHtml);
    }
    
    private void showError(String errorMessage) {
        String sanitizedMsg = escapeHtmlEntities(errorMessage);
        if (hasModernBrowserSupport) {
            String bootstrapAlert = 
                "<div class='alert alert-danger shadow-sm' role='alert'>" +
                "<div class='d-flex align-items-center'>" +
                "<span class='fs-3 me-3'>⚠️</span>" +
                "<div><strong>Error Occurred</strong><hr class='my-2'/><p class='mb-0'>" + 
                sanitizedMsg + "</p></div></div></div>";
            webViewComponent.loadHTML(constructBootstrapPage(bootstrapAlert));
        } else {
            String basicError = 
                "<html><head><style>" +
                "body{font-family:Arial,sans-serif;padding:12px;margin:0;" +
                "background-color:#f8d7da;color:#721c24;border:2px solid #f5c6cb;}" +
                "</style></head><body><strong>⚠️ Error:</strong> " + sanitizedMsg + "</body></html>";
            legacyHtmlPane.setText(basicError);
        }
    }
    
    private void showInfo(String message) {
        String sanitizedMsg = escapeHtmlEntities(message);
        if (hasModernBrowserSupport) {
            String bootstrapAlert = 
                "<div class='alert alert-info shadow-sm' role='status'>" +
                "<div class='d-flex align-items-center'>" +
                "<span class='fs-3 me-3'>ℹ️</span>" +
                "<div><strong>Information</strong><hr class='my-2'/><p class='mb-0'>" + 
                sanitizedMsg + "</p></div></div></div>";
            webViewComponent.loadHTML(constructBootstrapPage(bootstrapAlert));
        } else {
            String basicInfo = 
                "<html><head><style>" +
                "body{font-family:Arial,sans-serif;padding:12px;margin:0;" +
                "background-color:#d1ecf1;color:#0c5460;border:2px solid #bee5eb;}" +
                "</style></head><body><strong>ℹ️ Info:</strong> " + sanitizedMsg + "</body></html>";
            legacyHtmlPane.setText(basicInfo);
        }
    }
    
    /**
     * Display a message indicating that the selected method requires arguments.
     * Uses HtmlTemplates for consistent styling.
     */
    private void showMethodRequiresArgumentsMessage() {
        String message = HtmlTemplates.getMethodRequiresArgumentsPage();
        if (hasModernBrowserSupport) {
            webViewComponent.loadHTML(constructBootstrapPage(message));
        } else {
            // Fallback for environments without JCEF
            String basicMessage = 
                "<html><head><style>" +
                "body{font-family:Arial,sans-serif;padding:12px;margin:0;" +
                "background-color:#fff3cd;color:#856404;border:2px solid #ffeaa7;}" +
                "</style></head><body>" +
                "<strong>⚙️ Method Requires Arguments</strong><br/><br/>" +
                "The selected method requires parameters to be provided.<br/><br/>" +
                "<strong>Next steps:</strong><br/>" +
                "1. Set the arguments in the expression editor panel above<br/>" +
                "2. Click the ▶ (Compile and Preview) button to render the HTML" +
                "</body></html>";
            legacyHtmlPane.setText(basicMessage);
        }
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
     * Create a single-line EditorTextField for writing Java expressions.
     * Similar to debugger's "Evaluate Expression" input.
     */
    private EditorTextField createExpressionEditor() {
        Document document = EditorFactory.getInstance().createDocument("");
        
        EditorTextField textField = new EditorTextField(document, project, JavaFileType.INSTANCE) {
            @Override
            protected EditorEx createEditor() {
                EditorEx editor = super.createEditor();
                // Configure single-line mode at the editor level
                editor.getSettings().setUseSoftWraps(false);
                editor.setOneLineMode(true);
                return editor;
            }
        };
        
        // Also set one-line mode at the text field level for complete consistency
        // This ensures the mode is enforced both when the editor is created and at the field level
        textField.setOneLineMode(true);
        
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
     * E.g., "ClassName.userCard(new User(\"\", \"\"), \"\")" for static methods
     * or "methodName(...)" for instance methods.
     */
    private String buildMethodCallTemplate(PsiMethod method) {
        StringBuilder sb = new StringBuilder();
        
        // For static methods, prefix with class name to make them accessible
        // This allows calling methods from the same class without static imports
        if (method.hasModifierProperty(PsiModifier.STATIC)) {
            PsiClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                sb.append(containingClass.getName()).append(".");
            }
        }
        
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
        
        // Throttle compilation requests
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCompilationTime < COMPILATION_THROTTLE_MS) {
            showInfo("Please wait... compilation is still in progress or too soon since last compilation.");
            return;
        }
        lastCompilationTime = currentTime;
        
        // Create fragment and get module
        // These are quick operations, can be done on EDT with ReadAction
        JavaCodeFragmentFactory fragmentFactory = JavaCodeFragmentFactory.getInstance(project);
        PsiExpressionCodeFragment fragment = fragmentFactory.createExpressionCodeFragment(
            expressionText,
            currentMethod.getContainingClass(),
            currentMethod.getReturnType(),
            true
        );
        
        Module module = ModuleUtilCore.findModuleForPsiElement(currentMethod);
        if (module == null) {
            showError("Could not find module");
            return;
        }
        
        showInfo("Compiling expression...");
        
        // CompilerManager.make() MUST be called on EDT
        CompilerManager.getInstance(project).make(module, new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext context) {
                if (aborted) {
                    showError("Compilation was aborted.");
                } else if (errors > 0) {
                    showError("Compilation failed with " + errors + " error(s). Check the Problems panel.");
                } else {
                    // Compilation succeeded - now evaluate the expression
                    // Move heavy compilation work to background thread
                    com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            evaluateAndDisplay(fragment, module);
                        } catch (Exception e) {
                            SwingUtilities.invokeLater(() -> {
                                showError("Error evaluating expression: " + e.getMessage());
                                LOG.error("Error evaluating expression", e);
                            });
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Evaluate the compiled expression and display the result.
     * Uses JavaCompiler API to compile and execute arbitrary Java expressions.
     */
    private void evaluateAndDisplay(PsiExpressionCodeFragment fragment, Module module) throws Exception {
        String expressionText = fragment.getText().trim();
        
        // Get package name from context
        String packageName = "";
        PsiElement context = fragment.getContext();
        if (context != null) {
            PsiJavaFile javaFile = (PsiJavaFile) context.getContainingFile();
            if (javaFile != null) {
                packageName = javaFile.getPackageName();
            }
        }
        
        // Generate wrapper class (simple name only)
        String simpleClassName = "ExpressionWrapper_" + System.currentTimeMillis();
        String wrapperCode = generateWrapperClass(simpleClassName, expressionText, fragment);
        
        // Full qualified name includes package
        String wrapperClassName = packageName.isEmpty() ? simpleClassName : packageName + "." + simpleClassName;
        
        // Get classpath for compilation
        String classpath = buildClasspath(module);
        
        // Compile the wrapper class (pass fully qualified name)
        Class<?> wrapperClass = compileAndLoadClass(wrapperClassName, wrapperCode, classpath, module);
        
        // Execute the eval() method
        Method evalMethod = wrapperClass.getDeclaredMethod("eval");
        evalMethod.setAccessible(true);
        Object result = evalMethod.invoke(null);
        
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
     * Generate a wrapper class that contains the expression as a method.
     * Includes all necessary imports from the context.
     */
    private String generateWrapperClass(String className, String expression, PsiExpressionCodeFragment fragment) {
        StringBuilder code = new StringBuilder();
        
        // Get the package from the context
        PsiElement context = fragment.getContext();
        PsiJavaFile javaFile = null;
        if (context != null) {
            javaFile = (PsiJavaFile) context.getContainingFile();
        }
        
        // Add package declaration
        if (javaFile != null && !javaFile.getPackageName().isEmpty()) {
            code.append("package ").append(javaFile.getPackageName()).append(";\n");
            code.append("\n");
        }
        
        // Add imports from the original file
        if (javaFile != null) {
            PsiImportList importList = javaFile.getImportList();
            if (importList != null) {
                for (PsiImportStatement importStatement : importList.getImportStatements()) {
                    code.append(importStatement.getText()).append("\n");
                }
                for (PsiImportStaticStatement importStatic : importList.getImportStaticStatements()) {
                    code.append(importStatic.getText()).append("\n");
                }
            }
            code.append("\n");
        }
        
        // Generate the wrapper class
        code.append("public class ").append(className).append(" {\n");
        code.append("    public static Object eval() {\n");
        code.append("        return ").append(expression).append(";\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    /**
     * Build the classpath string needed for JavaCompiler.
     * Includes all module dependencies and compiled output.
     */
    private String buildClasspath(Module module) {
        // Use LinkedHashSet to preserve order and automatically deduplicate
        Set<String> classpathEntries = new LinkedHashSet<>();
        
        System.err.println("=== BUILD CLASSPATH DEBUG ===");
        System.err.println("Building classpath for module: " + module.getName());
        LOG.info("Building classpath for module: " + module.getName());

        // Get all classpath roots (same as we did for the classloader)
        // Try without withoutSdk() to see if that helps
        VirtualFile[] roots = OrderEnumerator.orderEntries(module)
            .recursively()
            .classes()
            .getRoots();

        System.err.println("Found " + roots.length + " classpath roots from OrderEnumerator");
        LOG.info("Found " + roots.length + " classpath roots from OrderEnumerator");

        for (VirtualFile root : roots) {
            String path = root.getPath();
            System.err.println("Processing classpath entry: " + path);
            LOG.info("Processing classpath entry: " + path);

            // Clean up jar:// protocol and jar entry separators (!)
            // VirtualFile paths can be:
            // - jar://C:/path/file.jar!/entry/path (JAR with entry)
            // - jar://C:/path/jdk!\module.name (JDK module)
            // - /path/to/directory (regular directory)
            if (path.startsWith("jar://")) {
                path = path.substring(6);
                System.err.println("  Removed jar:// protocol: " + path);
                LOG.info("  Removed jar:// protocol: " + path);
            }
            
            // Remove jar entry separator (! or !/) and everything after it
            // This handles both "file.jar!/" and "jdk!\module" formats
            int exclamation = path.indexOf("!");
            if (exclamation != -1) {
                path = path.substring(0, exclamation);
                System.err.println("  Removed ! separator and entry: " + path);
                LOG.info("  Removed ! separator and entry: " + path);
            }

            // Convert to proper file system path
            // This handles Windows paths correctly (e.g., /C:/ becomes C:\)
            File file = new File(path);
            String normalizedPath = file.getAbsolutePath();
            
            System.err.println("  Normalized to: " + normalizedPath);
            System.err.println("  File exists: " + new File(normalizedPath).exists());
            LOG.info("  Normalized to: " + normalizedPath);
            LOG.info("  File exists: " + new File(normalizedPath).exists());

            classpathEntries.add(normalizedPath);
        }
        
        // Join with system path separator (; on Windows, : on Unix)
        String classpath = String.join(File.pathSeparator, classpathEntries);
        
        // Log the classpath for debugging
        System.err.println("Built classpath with " + classpathEntries.size() + " entries for compilation");
        System.err.println("Full classpath: " + classpath);
        System.err.println("=== END BUILD CLASSPATH DEBUG ===");
        LOG.info("Built classpath with " + classpathEntries.size() + " entries for compilation");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Full classpath: " + classpath);
        }
        
        return classpath;
    }
    
    /**
     * Get the JDK home path for the project's configured SDK.
     * Returns null if no SDK is configured or path is not available.
     */
    private String getProjectJdkHome() {
        try {
            Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
            if (projectSdk != null) {
                return projectSdk.getHomePath();
            }
        } catch (Exception e) {
            LOG.warn("Failed to get project JDK home", e);
        }
        return null;
    }
    
    /**
     * Get JavaCompiler from the project's configured JDK.
     * This works even if IntelliJ itself is running on a JRE.
     * Returns null if compiler API is not accessible (will fall back to process-based compilation).
     */
    private JavaCompiler getProjectJavaCompiler() throws Exception {
        // Get the project's configured SDK
        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (projectSdk == null) {
            throw new Exception("No JDK configured for this project. Please configure a JDK in File → Project Structure → Project Settings → Project → SDK.");
        }
        
        // Get the JDK home path
        String jdkHomePath = projectSdk.getHomePath();
        if (jdkHomePath == null) {
            throw new Exception("JDK home path not found for configured SDK: " + projectSdk.getName());
        }
        
        // Java 9+ approach: Compiler is part of the JDK, no tools.jar needed
        // Java 8 approach: Compiler is in tools.jar
        
        File jdkHome = new File(jdkHomePath);
        File toolsJar = new File(jdkHome, "lib/tools.jar");
        
        if (!toolsJar.exists()) {
            // Java 9+ path - no tools.jar exists
            // The compiler should be available via the system if running on this JDK
            
            // Try getting the system compiler first
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            
            if (compiler != null) {
                return compiler;
            }
            
            // If system compiler not available, return null to signal that we need
            // to use process-based compilation (compileViaProcess method)
            // This handles the case where IntelliJ runs on JRE but project uses Java 9+ JDK
            return null;
        }
        
        // Java 8 path: Load tools.jar to get the compiler
        try {
            // Note: This URLClassLoader must remain open for the lifetime of the compiler.
            // The compiler instance references classes from tools.jar, so closing the
            // classloader would break the compiler functionality.
            URLClassLoader loader = new URLClassLoader(
                new URL[]{toolsJar.toURI().toURL()},
                ClassLoader.getSystemClassLoader()
            );
            
            // Get ToolProvider class from tools.jar
            Class<?> toolProviderClass = loader.loadClass("javax.tools.ToolProvider");
            Method getCompilerMethod = toolProviderClass.getMethod("getSystemJavaCompiler");
            JavaCompiler compiler = (JavaCompiler) getCompilerMethod.invoke(null);
            
            if (compiler == null) {
                throw new Exception("Failed to obtain JavaCompiler from tools.jar at: " + toolsJar);
            }
            
            return compiler;
            
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | 
                 InvocationTargetException | MalformedURLException e) {
            throw new Exception("Failed to load Java compiler from project JDK: " + e.getMessage(), e);
        }
    }
    
    /**
     * Compile source code using process-based javac invocation.
     * This is a fallback when JavaCompiler API is not accessible (Java 9+ with IntelliJ on JRE).
     * 
     * @param sourceFile The Java source file to compile
     * @param classpath The classpath to use for compilation
     * @param outputDir The directory where compiled classes should be placed
     * @throws Exception if javac cannot be executed or compilation fails
     */
    private void compileViaProcess(Path sourceFile, String classpath, Path outputDir) throws Exception {
        String jdkHome = getProjectJdkHome();
        if (jdkHome == null) {
            throw new Exception("Cannot compile via process: JDK home path not available");
        }
        
        // Determine javac executable path
        File javacFile = new File(jdkHome, "bin/javac");
        if (!javacFile.exists()) {
            javacFile = new File(jdkHome, "bin/javac.exe"); // Windows
        }
        
        if (!javacFile.exists()) {
            throw new Exception("javac not found at: " + javacFile.getAbsolutePath());
        }
        
        // Build command
        List<String> command = new ArrayList<>();
        command.add(javacFile.getAbsolutePath());
        command.add("-source");
        command.add("17");  // Compile for Java 17 (IntelliJ's runtime version)
        command.add("-target");
        command.add("17");  // Target Java 17 bytecode
        command.add("-classpath");
        command.add(classpath);
        command.add("-d");
        command.add(outputDir.toString());
        command.add(sourceFile.toString());
        
        // Log the command for debugging
        System.err.println("=== JAVAC COMMAND DEBUG ===");
        System.err.println("Executing javac command:");
        System.err.println("  javac: " + javacFile.getAbsolutePath());
        System.err.println("  -classpath: " + classpath);
        System.err.println("  -d: " + outputDir.toString());
        System.err.println("  source: " + sourceFile.toString());
        System.err.println("Full command: " + String.join(" ", command));
        System.err.println("=== END JAVAC COMMAND DEBUG ===");
        LOG.info("Executing javac command: " + String.join(" ", command));
        
        // Execute javac
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        try {
            Process process = pb.start();
            
            // Read output
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (java.io.IOException e) {
                throw new Exception("Failed to read javac output: " + e.getMessage(), e);
            }
            
            int exitCode = process.waitFor();
            
            System.err.println("=== JAVAC RESULT DEBUG ===");
            System.err.println("Exit code: " + exitCode);
            System.err.println("Output: " + output.toString());
            System.err.println("=== END JAVAC RESULT DEBUG ===");
            
            if (exitCode != 0) {
                throw new Exception("Compilation failed:\n" + output.toString());
            }
            
            // Verify .class file was created
            System.err.println("=== COMPILED FILES DEBUG ===");
            try {
                Files.walk(outputDir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> System.err.println("  Found: " + file));
            } catch (java.io.IOException e) {
                System.err.println("  Failed to list files: " + e.getMessage());
            }
            System.err.println("=== END COMPILED FILES DEBUG ===");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Compilation interrupted", e);
        } catch (java.io.IOException e) {
            throw new Exception("Failed to start javac process: " + e.getMessage(), e);
        }
    }
    
    /**
     * Compile the wrapper class source code and load it into memory.
     * Uses JavaCompiler API for runtime compilation.
     * 
     * Note on resource management:
     * - Temporary files are marked with deleteOnExit() for cleanup on JVM shutdown.
     *   This is acceptable for a development tool with short-lived IDE sessions.
     * - The URLClassLoader is not explicitly closed because the loaded class needs
     *   to remain accessible for the duration of the preview display. The classloader
     *   will be garbage collected when no longer referenced.
     * - For production use, consider implementing explicit cleanup of old classloaders
     *   and temporary directories.
     */
    private Class<?> compileAndLoadClass(String className, String sourceCode, String classpath, Module module) 
            throws Exception {
        
        // Use the project's configured JDK to get the compiler
        JavaCompiler compiler = getProjectJavaCompiler();
        
        // Create temporary directory for compilation
        // Note: deleteOnExit() is used for automatic cleanup. In a long-running server
        // context, consider explicit cleanup to avoid memory leaks from JVM's internal list.
        Path tempDir = Files.createTempDirectory("j2html_expr_");
        tempDir.toFile().deleteOnExit();
        
        // Determine package path
        String packagePath = "";
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packagePath = className.substring(0, lastDot).replace('.', '/');
            className = className.substring(lastDot + 1);
        }
        
        // Create source file
        Path sourceDir = tempDir;
        if (!packagePath.isEmpty()) {
            sourceDir = tempDir.resolve(packagePath);
            Files.createDirectories(sourceDir);
        }
        
        // Write source code to temporary file
        // Note: File permissions are set by default. For multi-user systems in production,
        // consider using Files.setPosixFilePermissions() to restrict access.
        Path sourceFile = sourceDir.resolve(className + ".java");
        Files.writeString(sourceFile, sourceCode);
        
        // Check if we have a JavaCompiler or need to use process-based compilation
        if (compiler != null) {
            // Use JavaCompiler API
            // Prepare compilation options
            List<String> options = new ArrayList<>();
            options.add("-source");
            options.add("17");  // Compile for Java 17 (IntelliJ's runtime version)
            options.add("-target");
            options.add("17");  // Target Java 17 bytecode
            options.add("-classpath");
            options.add(classpath);
            options.add("-d");
            options.add(tempDir.toString());
            
            // Get diagnostic collector to capture compilation errors
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            
            // Get compilation units
            Iterable<? extends JavaFileObject> compilationUnits = 
                fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile.toFile()));
            
            // Compile
            JavaCompiler.CompilationTask task = compiler.getTask(
                null,                    // Writer for additional output
                fileManager,            // File manager
                diagnostics,            // Diagnostic listener
                options,                // Compiler options
                null,                   // Classes for annotation processing
                compilationUnits        // Compilation units
            );
            
            boolean success = task.call();
            fileManager.close();
            
            if (!success) {
                // Compilation failed - format error messages
                StringBuilder errors = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errors.append(String.format("Line %d: %s\n", 
                        diagnostic.getLineNumber(), 
                        diagnostic.getMessage(null)));
                }
                throw new Exception(errors.toString());
            }
        } else {
            // Fall back to process-based compilation (Java 9+ with IntelliJ on JRE)
            LOG.info("Using process-based javac compilation (JavaCompiler API not available)");
            compileViaProcess(sourceFile, classpath, tempDir);
        }
        
        // Load the compiled class
        // Note: URLClassLoader is not closed here because the returned Class object
        // needs the classloader to remain alive. The classloader will be GC'd when
        // the class and its instances are no longer referenced.
        URLClassLoader classLoader = new URLClassLoader(
            new URL[]{tempDir.toUri().toURL()},
            getModuleClassLoader(module)
        );
        
        // Full class name includes package if present
        String fullClassName = packagePath.isEmpty() ? className : packagePath.replace('/', '.') + '.' + className;
        return classLoader.loadClass(fullClassName);
    }
    
    private void renderToView(String htmlContent) {
        if (hasModernBrowserSupport) {
            webViewComponent.loadHTML(constructBootstrapPage(htmlContent));
        } else {
            legacyHtmlPane.setText(htmlContent);
        }
    }
    
    private String constructBootstrapPage(String bodyContent) {
        StringBuilder pageBuilder = new StringBuilder();
        pageBuilder.append("<!DOCTYPE html>\n");
        pageBuilder.append("<html lang='en'>\n");
        pageBuilder.append("<head>\n");
        pageBuilder.append("<meta charset='UTF-8'>\n");
        pageBuilder.append("<meta name='viewport' content='width=device-width, initial-scale=1'>\n");
        pageBuilder.append("<title>j2html Preview</title>\n");
        
        // Bootstrap CSS from jsDelivr CDN
        pageBuilder.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' ");
        pageBuilder.append("rel='stylesheet' ");
        pageBuilder.append("integrity='sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH' ");
        pageBuilder.append("crossorigin='anonymous'>\n");
        
        pageBuilder.append("</head>\n");
        pageBuilder.append("<body>\n");
        pageBuilder.append("<div class='container-fluid p-3'>\n");
        pageBuilder.append(bodyContent);
        pageBuilder.append("\n</div>\n");
        
        // Bootstrap JS bundle from jsDelivr CDN
        pageBuilder.append("<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js' ");
        pageBuilder.append("integrity='sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz' ");
        pageBuilder.append("crossorigin='anonymous'></script>\n");
        
        pageBuilder.append("</body>\n");
        pageBuilder.append("</html>");
        
        return pageBuilder.toString();
    }
    
    private String escapeHtmlEntities(String rawText) {
        if (rawText == null) {
            return "";
        }
        char[] textChars = rawText.toCharArray();
        StringBuilder escapedBuilder = new StringBuilder(rawText.length() + 50);
        
        for (char ch : textChars) {
            switch (ch) {
                case '<': escapedBuilder.append("&lt;"); break;
                case '>': escapedBuilder.append("&gt;"); break;
                case '&': escapedBuilder.append("&amp;"); break;
                case '"': escapedBuilder.append("&quot;"); break;
                case '\'': escapedBuilder.append("&#39;"); break;
                default: escapedBuilder.append(ch);
            }
        }
        
        return escapedBuilder.toString();
    }
    
    /**
     * Phase 5c: Save the current expression as a @Preview annotated method.
     * Prompts the user for a name and generates the method in the source file.
     */
    private void saveAsPreview() {
        if (currentMethod == null) {
            showError("No method selected");
            return;
        }
        
        String expressionText = expressionEditor.getText().trim();
        if (expressionText.isEmpty()) {
            showError("Expression is empty");
            return;
        }
        
        // Prompt user for preview name
        String previewName = JOptionPane.showInputDialog(
            this,
            "Enter a name for this preview:",
            "Save as @Preview",
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (previewName == null || previewName.trim().isEmpty()) {
            // User cancelled or entered empty name
            return;
        }
        
        previewName = previewName.trim();
        
        // Generate method name from preview name
        String methodName = generateMethodName(previewName);
        
        // Get return type from current method
        PsiType returnType = currentMethod.getReturnType();
        if (returnType == null) {
            showError("Cannot determine return type");
            return;
        }
        
        String returnTypeName = returnType.getPresentableText();
        
        // Generate the method code
        String methodCode = generatePreviewMethod(previewName, methodName, returnTypeName, expressionText);
        
        // Insert the method into the source file
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    insertPreviewMethod(currentMethod.getContainingClass(), methodCode);
                    showInfo("Preview method '" + methodName + "' created successfully!");
                } catch (Exception e) {
                    showError("Failed to create preview method: " + e.getMessage());
                    LOG.error("Failed to create preview method", e);
                }
            });
        });
    }
    
    /**
     * Generate a valid Java method name from a preview name.
     * Converts spaces and special characters to underscores, and converts to camelCase.
     */
    private String generateMethodName(String previewName) {
        // Start with the base name from current method
        String baseName = currentMethod.getName();
        
        // Remove common suffixes if present
        if (baseName.endsWith("Tag")) {
            baseName = baseName.substring(0, baseName.length() - 3);
        }
        
        // Convert preview name to a valid identifier suffix
        String suffix = previewName
            .replaceAll("[^a-zA-Z0-9]", "_")  // Replace non-alphanumeric with underscore
            .replaceAll("_+", "_")             // Collapse multiple underscores
            .replaceAll("^_|_$", "")           // Remove leading/trailing underscores
            .toLowerCase();
        
        // Combine base name with suffix
        String methodName = baseName + "_" + suffix;
        
        // Ensure uniqueness in the class
        PsiClass containingClass = currentMethod.getContainingClass();
        if (containingClass != null) {
            // Collect all method names once for efficiency
            Set<String> existingMethodNames = new LinkedHashSet<>();
            for (PsiMethod method : containingClass.getMethods()) {
                existingMethodNames.add(method.getName());
            }
            
            // Find unique method name
            String finalMethodName = methodName;
            int counter = 1;
            while (existingMethodNames.contains(finalMethodName)) {
                finalMethodName = methodName + counter;
                counter++;
            }
            methodName = finalMethodName;
        }
        
        return methodName;
    }
    
    /**
     * Generate the complete @Preview annotated method code.
     */
    private String generatePreviewMethod(String previewName, String methodName, String returnTypeName, String expressionText) {
        StringBuilder code = new StringBuilder();
        
        // Add JavaDoc comment (no leading indentation for PSI factory)
        code.append("/**\n");
        code.append(" * Preview: ").append(previewName).append("\n");
        code.append(" */\n");

        // Add @Preview annotation (escape backslashes first, then quotes)
        String escapedName = previewName.replace("\\", "\\\\").replace("\"", "\\\"");
        code.append("@Preview(name = \"").append(escapedName).append("\")\n");

        // Add method signature
        code.append("public static ").append(returnTypeName).append(" ").append(methodName).append("() {\n");

        // Add method body - return the expression
        code.append("    return ").append(expressionText);
        if (!expressionText.endsWith(";")) {
            code.append(";");
        }
        code.append("\n");
        
        code.append("}\n");

        return code.toString();
    }
    
    /**
     * Insert the generated method into the containing class.
     * Uses PSI manipulation to add the method at the end of the class.
     */
    private void insertPreviewMethod(PsiClass psiClass, String methodCode) {
        if (psiClass == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        
        // Create a method from the code
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiMethod newMethod = factory.createMethodFromText(methodCode, psiClass);
        
        // Add the method to the class
        psiClass.add(newMethod);
        
        // Format the code
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        codeStyleManager.reformat(newMethod);
    }
}
