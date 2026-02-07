package com.example.j2htmlpreview;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the j2html Preview tool window.
 * Phase 2: Now delegates to PreviewPanel for UI logic.
 */
public class J2HtmlPreviewToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create the refactored preview panel with modular architecture
        PreviewPanelRefactored previewPanel = new PreviewPanelRefactored(project);
        
        // Wrap it in a Content object and add to the tool window
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(previewPanel, "", false);
        content.setDisposer(previewPanel);  // Register disposable to prevent memory leaks
        toolWindow.getContentManager().addContent(content);
    }
}
