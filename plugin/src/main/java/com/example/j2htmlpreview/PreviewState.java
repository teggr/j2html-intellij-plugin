package com.example.j2htmlpreview;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiMethod;
import com.intellij.util.messages.Topic;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared state model for the preview panel.
 * Manages application state and publishes events via IntelliJ's message bus system.
 * Components subscribe to state changes through the PreviewStateListener interface.
 */
public class PreviewState {
    
    /**
     * Status of the preview panel.
     */
    public enum PreviewStatus {
        IDLE,       // Ready for user interaction
        COMPILING,  // Code is being compiled
        RENDERING,  // HTML is being rendered
        ERROR,      // An error occurred
        SUCCESS     // Operation completed successfully
    }
    
    /**
     * Topic for publishing state change events via IntelliJ's message bus.
     * Components subscribe to this topic to receive notifications.
     */
    public static final Topic<PreviewStateListener> TOPIC = 
        Topic.create("PreviewState", PreviewStateListener.class);
    
    // State fields
    private VirtualFile currentFile;
    private List<PsiMethod> j2htmlMethods = new ArrayList<>();
    private PsiMethod selectedMethod;
    private String currentHtml = "";
    private PreviewStatus status = PreviewStatus.IDLE;
    private String statusMessage = "";
    
    /**
     * Listener interface for state change events.
     * Components implement this to react to state changes.
     */
    public interface PreviewStateListener {
        /**
         * Called when the current file changes.
         */
        default void onFileChanged(VirtualFile file) {}
        
        /**
         * Called when the list of j2html methods changes.
         */
        default void onMethodsChanged(List<PsiMethod> methods) {}
        
        /**
         * Called when a method is selected.
         */
        default void onMethodSelected(PsiMethod method) {}
        
        /**
         * Called when the HTML content changes.
         */
        default void onHtmlChanged(String html) {}
        
        /**
         * Called when the status changes.
         */
        default void onStatusChanged(PreviewStatus status, String message) {}
    }
    
    // Getters
    
    public VirtualFile getCurrentFile() {
        return currentFile;
    }
    
    public List<PsiMethod> getJ2htmlMethods() {
        return new ArrayList<>(j2htmlMethods);
    }
    
    public PsiMethod getSelectedMethod() {
        return selectedMethod;
    }
    
    public String getCurrentHtml() {
        return currentHtml;
    }
    
    public PreviewStatus getStatus() {
        return status;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    // Setters (package-private - only accessible within the same package)
    
    void setCurrentFile(VirtualFile file) {
        this.currentFile = file;
    }
    
    void setJ2htmlMethods(List<PsiMethod> methods) {
        this.j2htmlMethods = new ArrayList<>(methods);
    }
    
    void setSelectedMethod(PsiMethod method) {
        this.selectedMethod = method;
    }
    
    void setCurrentHtml(String html) {
        this.currentHtml = html;
    }
    
    void setStatus(PreviewStatus status, String message) {
        this.status = status;
        this.statusMessage = message;
    }
}
