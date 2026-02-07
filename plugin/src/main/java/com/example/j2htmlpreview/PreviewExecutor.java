package com.example.j2htmlpreview;

import com.intellij.psi.PsiMethod;
import com.intellij.util.messages.Topic;

/**
 * Service interface for handling j2html method compilation and execution.
 * Components publish execution requests via the message bus, and the executor
 * handles the compilation, execution, and result publishing.
 */
public interface PreviewExecutor {
    
    /**
     * Topic for publishing execution requests via IntelliJ's message bus.
     */
    Topic<PreviewExecutorListener> TOPIC = 
        Topic.create("PreviewExecutor", PreviewExecutorListener.class);
    
    /**
     * Listener interface for execution requests.
     */
    interface PreviewExecutorListener {
        /**
         * Called when a zero-parameter method should be executed.
         */
        default void onExecuteMethod(PsiMethod method) {}
        
        /**
         * Called when a parameterized expression should be evaluated.
         */
        default void onExecuteExpression(PsiMethod contextMethod, String expression) {}
    }
}
