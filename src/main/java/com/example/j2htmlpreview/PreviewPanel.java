package com.example.j2htmlpreview;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main UI panel for the j2html preview tool window.
 * Phase 3: Finds and lists methods that return j2html types.
 */
public class PreviewPanel extends JPanel {
    private final Project project;
    private final JLabel currentFileLabel;
    private final JComboBox<String> methodSelector;
    private final JEditorPane htmlPreview;
    private final List<PsiMethod> j2htmlMethods = new ArrayList<>();
    
    public PreviewPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("j2html Preview - Phase 3");
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
    
    private void updateCurrentFile() {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        VirtualFile currentFile = editorManager.getSelectedFiles().length > 0 
            ? editorManager.getSelectedFiles()[0] 
            : null;
        
        if (currentFile != null) {
            currentFileLabel.setText("Current file: " + currentFile.getName());
            analyzeFile(currentFile);
        } else {
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
     */
    private void onMethodSelected() {
        int selectedIndex = methodSelector.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < j2htmlMethods.size()) {
            PsiMethod selectedMethod = j2htmlMethods.get(selectedIndex);
            htmlPreview.setText(getMethodSelectedHtml(selectedMethod));
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
}
