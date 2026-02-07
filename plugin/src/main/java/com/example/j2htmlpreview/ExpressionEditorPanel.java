package com.example.j2htmlpreview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.util.messages.MessageBusConnection;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * UI component for editing Java expressions.
 * Shows/hides based on whether selected method has parameters.
 * Provides Execute and Save as Preview functionality.
 */
public class ExpressionEditorPanel extends JPanel implements Disposable {
    
    private static final Logger LOG = Logger.getInstance(ExpressionEditorPanel.class);
    
    private final Project project;
    private EditorTextField expressionEditor;
    private PsiExpressionCodeFragment currentFragment;
    private PsiMethod currentMethod;
    private MessageBusConnection connection;
    
    public ExpressionEditorPanel(Project project) {
        this.project = project;
        
        setLayout(new BorderLayout(5, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Create expression editor
        expressionEditor = createExpressionEditor();
        
        // Create execute button
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
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(executeButton);
        buttonPanel.add(saveAsPreviewButton);
        
        add(expressionEditor, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
        
        // Initially hidden
        setVisible(false);
        
        // Subscribe to state changes
        setupStateListener();
    }
    
    private EditorTextField createExpressionEditor() {
        Document document = EditorFactory.getInstance().createDocument("");
        
        EditorTextField textField = new EditorTextField(document, project, JavaFileType.INSTANCE) {
            @Override
            protected EditorEx createEditor() {
                EditorEx editor = super.createEditor();
                editor.getSettings().setUseSoftWraps(false);
                editor.setOneLineMode(true);
                return editor;
            }
        };
        
        textField.setOneLineMode(true);
        return textField;
    }
    
    private void setupStateListener() {
        connection = project.getMessageBus().connect(this);
        connection.subscribe(PreviewState.TOPIC, new PreviewState.PreviewStateListener() {
            @Override
            public void onMethodSelected(PsiMethod method) {
                updateForMethod(method);
            }
        });
    }
    
    private void updateForMethod(PsiMethod method) {
        currentMethod = method;
        
        if (method == null || !ExpressionTemplateBuilder.hasParameters(method)) {
            // No parameters - hide editor
            setVisible(false);
        } else {
            // Has parameters - show editor with template
            setVisible(true);
            populateExpressionEditor(method);
        }
    }
    
    private void populateExpressionEditor(PsiMethod method) {
        String template = ExpressionTemplateBuilder.buildExpressionTemplate(method);
        
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
    
    private void executeExpression() {
        if (currentMethod == null) {
            publishError("No method selected");
            return;
        }
        
        String expressionText = expressionEditor.getText().trim();
        if (expressionText.isEmpty()) {
            publishError("Expression is empty");
            return;
        }
        
        // Publish execution request via message bus
        // The PreviewExecutor will handle the actual compilation and execution
        project.getMessageBus()
            .syncPublisher(PreviewExecutor.TOPIC)
            .onExecuteExpression(currentMethod, expressionText);
    }
    
    private void saveAsPreview() {
        if (currentMethod == null) {
            publishError("No method selected");
            return;
        }
        
        String expressionText = expressionEditor.getText().trim();
        if (expressionText.isEmpty()) {
            publishError("Expression is empty");
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
            return;
        }
        
        previewName = previewName.trim();
        
        // Generate method name from preview name
        String methodName = generateMethodName(previewName);
        
        // Get return type from current method
        PsiType returnType = currentMethod.getReturnType();
        if (returnType == null) {
            publishError("Cannot determine return type");
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
                    publishInfo("Preview method '" + methodName + "' created successfully!");
                } catch (Exception e) {
                    publishError("Failed to create preview method: " + e.getMessage());
                    LOG.error("Failed to create preview method", e);
                }
            });
        });
    }
    
    private String generateMethodName(String previewName) {
        String baseName = currentMethod.getName();
        
        // Remove common suffixes if present
        if (baseName.endsWith("Tag")) {
            baseName = baseName.substring(0, baseName.length() - 3);
        }
        
        // Convert preview name to a valid identifier suffix
        String suffix = previewName
            .replaceAll("[^a-zA-Z0-9]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "")
            .toLowerCase();
        
        String methodName = baseName + "_" + suffix;
        
        // Ensure uniqueness in the class
        PsiClass containingClass = currentMethod.getContainingClass();
        if (containingClass != null) {
            Set<String> existingMethodNames = new LinkedHashSet<>();
            for (PsiMethod method : containingClass.getMethods()) {
                existingMethodNames.add(method.getName());
            }
            
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
    
    private String generatePreviewMethod(String previewName, String methodName, String returnTypeName, String expressionText) {
        StringBuilder code = new StringBuilder();
        
        code.append("/**\n");
        code.append(" * Preview: ").append(previewName).append("\n");
        code.append(" */\n");
        
        String escapedName = previewName.replace("\\", "\\\\").replace("\"", "\\\"");
        code.append("@Preview(name = \"").append(escapedName).append("\")\n");
        
        code.append("public static ").append(returnTypeName).append(" ").append(methodName).append("() {\n");
        code.append("    return ").append(expressionText);
        if (!expressionText.endsWith(";")) {
            code.append(";");
        }
        code.append("\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    private void insertPreviewMethod(PsiClass psiClass, String methodCode) {
        if (psiClass == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiMethod newMethod = factory.createMethodFromText(methodCode, psiClass);
        
        psiClass.add(newMethod);
        
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        codeStyleManager.reformat(newMethod);
    }
    
    private void publishError(String message) {
        project.getMessageBus()
            .syncPublisher(PreviewState.TOPIC)
            .onStatusChanged(PreviewState.PreviewStatus.ERROR, message);
    }
    
    private void publishInfo(String message) {
        project.getMessageBus()
            .syncPublisher(PreviewState.TOPIC)
            .onStatusChanged(PreviewState.PreviewStatus.SUCCESS, message);
    }
    
    @Override
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
        }
    }
}
