package com.example.j2htmlpreview;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the j2html Preview tool window.
 * Phase 4: Dynamic execution with compile-on-demand.
 */
public class J2HtmlPreviewToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create the preview panel with all Phase 4 functionality
        PreviewPanel previewPanel = new PreviewPanel(project);
        
        // Create and add content to tool window
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(previewPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
