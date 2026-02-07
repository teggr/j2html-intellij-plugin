package com.example.j2htmlpreview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.util.messages.MessageBusConnection;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * UI component for selecting j2html methods.
 * Displays a dropdown of discovered methods and publishes selection events.
 */
public class MethodSelectorPanel extends JPanel implements Disposable {
    
    private final Project project;
    private final JComboBox<String> methodSelector;
    private List<PsiMethod> j2htmlMethods;
    private MessageBusConnection connection;
    private boolean isUpdating = false; // Prevents infinite event loops
    
    public MethodSelectorPanel(Project project) {
        this.project = project;
        this.methodSelector = new JComboBox<>();
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel selectorPanel = new JPanel(new BorderLayout(5, 0));
        selectorPanel.add(new JLabel("Select method:"), BorderLayout.WEST);
        selectorPanel.add(methodSelector, BorderLayout.CENTER);
        
        add(selectorPanel, BorderLayout.NORTH);
        
        // Set up action listener
        methodSelector.addActionListener(e -> {
            if (!isUpdating) {
                onMethodSelected();
            }
        });
        
        // Subscribe to state changes
        setupStateListener();
    }
    
    private void setupStateListener() {
        connection = project.getMessageBus().connect(this);
        connection.subscribe(PreviewState.TOPIC, new PreviewState.PreviewStateListener() {
            @Override
            public void onMethodsChanged(List<PsiMethod> methods) {
                updateMethodList(methods);
            }
        });
    }
    
    private void updateMethodList(List<PsiMethod> methods) {
        this.j2htmlMethods = methods;
        
        // Remember current selection if possible
        String previousSelection = null;
        if (methodSelector.getSelectedIndex() >= 0) {
            previousSelection = (String) methodSelector.getSelectedItem();
        }
        
        // Prevent ActionListener from firing during update
        isUpdating = true;
        
        try {
            methodSelector.removeAllItems();
            
            if (methods.isEmpty()) {
                methodSelector.setEnabled(false);
                return;
            }
            
            methodSelector.setEnabled(true);
            
            // Populate with method signatures
            for (PsiMethod method : methods) {
                String signature = buildMethodSignature(method);
                methodSelector.addItem(signature);
            }
            
            // Try to restore previous selection
            if (previousSelection != null) {
                for (int i = 0; i < methodSelector.getItemCount(); i++) {
                    if (previousSelection.equals(methodSelector.getItemAt(i))) {
                        methodSelector.setSelectedIndex(i);
                        return;
                    }
                }
            }
            
            // Default: select first item
            if (methodSelector.getItemCount() > 0) {
                methodSelector.setSelectedIndex(0);
            }
        } finally {
            isUpdating = false;
        }
        
        // Trigger selection event manually after updating is complete
        if (methodSelector.getItemCount() > 0) {
            SwingUtilities.invokeLater(this::onMethodSelected);
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
    
    private void onMethodSelected() {
        int selectedIndex = methodSelector.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < j2htmlMethods.size()) {
            PsiMethod selectedMethod = j2htmlMethods.get(selectedIndex);
            
            // Publish method selection event
            project.getMessageBus()
                .syncPublisher(PreviewState.TOPIC)
                .onMethodSelected(selectedMethod);
        }
    }
    
    @Override
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
        }
    }
}
