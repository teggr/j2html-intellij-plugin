package com.example.j2htmlpreview;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Main preview panel for j2html components.
 * Phase 4: Method detection, selection, compilation, and execution with HTML rendering.
 */
public class PreviewPanel extends JPanel {
    
    private final Project project;
    private final JComboBox<String> methodSelector;
    private final JEditorPane htmlPreview;
    private List<PsiMethod> j2htmlMethods;
    
    public PreviewPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.j2htmlMethods = new ArrayList<>();
        
        // Create header with method selector
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("j2html Preview - Phase 4");
        headerLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        // Method selector dropdown
        methodSelector = new JComboBox<>();
        methodSelector.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        methodSelector.addActionListener(e -> onMethodSelected());
        headerPanel.add(methodSelector, BorderLayout.CENTER);
        
        // HTML preview pane
        htmlPreview = new JEditorPane("text/html", "");
        htmlPreview.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(htmlPreview);
        
        // Add components
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Initial state
        showInfo("Open a Java file with j2html methods to preview them.");
        
        // Start listening for file changes
        startFileListener();
    }
    
    /**
     * Start listening for active file changes to detect j2html methods.
     */
    private void startFileListener() {
        // Simple initial implementation - detect methods in current file
        refreshMethods();
    }
    
    /**
     * Refresh the list of j2html methods from the currently open file.
     */
    public void refreshMethods() {
        j2htmlMethods.clear();
        methodSelector.removeAllItems();
        
        VirtualFile currentFile = getCurrentFile();
        if (currentFile == null) {
            showInfo("No file is currently open.");
            return;
        }
        
        PsiFile psiFile = PsiManager.getInstance(project).findFile(currentFile);
        if (!(psiFile instanceof PsiJavaFile)) {
            showInfo("Current file is not a Java file.");
            return;
        }
        
        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        
        // Find all static methods that return j2html types
        for (PsiClass psiClass : javaFile.getClasses()) {
            for (PsiMethod method : psiClass.getMethods()) {
                if (isJ2HtmlMethod(method)) {
                    j2htmlMethods.add(method);
                    methodSelector.addItem(method.getName() + "()");
                }
            }
        }
        
        if (j2htmlMethods.isEmpty()) {
            showInfo("No j2html methods found in current file.");
        } else {
            showInfo("Found " + j2htmlMethods.size() + " j2html method(s). Select one to preview.");
        }
    }
    
    /**
     * Check if a method is a j2html preview method.
     * Must be: public, static, zero parameters, returns j2html tag type.
     */
    private boolean isJ2HtmlMethod(PsiMethod method) {
        // Must be public and static
        if (!method.hasModifierProperty(PsiModifier.PUBLIC) || 
            !method.hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }
        
        // Must have no parameters
        if (method.getParameterList().getParametersCount() != 0) {
            return false;
        }
        
        // Return type must be a j2html tag type (DomContent or subtype)
        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return false;
        }
        
        String returnTypeName = returnType.getPresentableText();
        return returnTypeName.endsWith("Tag") || 
               returnTypeName.equals("DomContent") ||
               returnTypeName.equals("ContainerTag");
    }
    
    /**
     * Get the currently open file in the editor.
     */
    private VirtualFile getCurrentFile() {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        VirtualFile[] selectedFiles = editorManager.getSelectedFiles();
        return selectedFiles.length > 0 ? selectedFiles[0] : null;
    }
    
    /**
     * Called when user selects a method from the dropdown.
     * Now triggers compilation first, then executes.
     */
    private void onMethodSelected() {
        int selectedIndex = methodSelector.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < j2htmlMethods.size()) {
            PsiMethod selectedMethod = j2htmlMethods.get(selectedIndex);
            compileAndExecute(selectedMethod);
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
        // Get the module that contains the method
        Module module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
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
    }
    
    /**
     * Execute the j2html method and render its output.
     */
    private void executeMethod(PsiMethod psiMethod) {
        try {
            // Get the fully qualified class name
            PsiClass containingClass = psiMethod.getContainingClass();
            if (containingClass == null) {
                showError("Could not find containing class for method");
                return;
            }
            
            String className = containingClass.getQualifiedName();
            String methodName = psiMethod.getName();
            
            // Load the compiled class
            Module module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
            if (module == null) {
                showError("Could not find module for class");
                return;
            }
            
            // Create classloader with module output path
            ClassLoader classLoader = createModuleClassLoader(module);
            Class<?> clazz = Class.forName(className, true, classLoader);
            
            // Invoke the static method
            Method method = clazz.getMethod(methodName);
            Object result = method.invoke(null);
            
            // Render the result
            if (result != null) {
                String html = result.toString();
                showSuccess(html);
            } else {
                showError("Method returned null");
            }
            
        } catch (ClassNotFoundException e) {
            showError("Class not found. Make sure the project is compiled: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            showError("Method not found: " + e.getMessage());
        } catch (Exception e) {
            showError("Error executing method: " + e.getMessage());
        }
    }
    
    /**
     * Create a classloader for the module's output directory.
     */
    private ClassLoader createModuleClassLoader(Module module) throws Exception {
        // Get module output path
        String outputPath = module.getModuleFilePath().replace(".iml", "/out/production/" + module.getName());
        URL url = new URL("file://" + outputPath + "/");
        return new URLClassLoader(new URL[]{url}, getClass().getClassLoader());
    }
    
    /**
     * Display successful HTML output in the preview pane.
     */
    private void showSuccess(String html) {
        String successHtml = """
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
                        margin-bottom: 16px;
                        color: #155724;
                        font-weight: bold;
                    }
                    .rendered-content {
                        border: 1px solid #ddd;
                        border-radius: 8px;
                        padding: 16px;
                        background: white;
                    }
                </style>
            </head>
            <body>
                <div class="success-banner">✓ Rendered successfully</div>
                <div class="rendered-content">
                    %s
                </div>
            </body>
            </html>
            """.formatted(html);
        
        htmlPreview.setText(successHtml);
    }
    
    /**
     * Display an error message in the preview pane.
     */
    private void showError(String message) {
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
                    <h3>⚠ Error</h3>
                    <p><code>%s</code></p>
                </div>
            </body>
            </html>
            """.formatted(message);
        
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
}
