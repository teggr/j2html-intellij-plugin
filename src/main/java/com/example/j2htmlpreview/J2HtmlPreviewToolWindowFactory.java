package com.example.j2htmlpreview;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Factory for creating the j2html Preview tool window.
 * Phase 1: Static HTML preview to validate plugin structure.
 */
public class J2HtmlPreviewToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Header label
        JLabel headerLabel = new JLabel("j2html Preview - Phase 1");
        headerLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        // HTML preview pane with static content
        JEditorPane htmlPreview = new JEditorPane("text/html", getStaticHtml());
        htmlPreview.setEditable(false);
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(htmlPreview);
        
        // Add components to main panel
        mainPanel.add(headerLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create and add content to tool window
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
    
    /**
     * Returns static HTML for Phase 1 demonstration.
     * Future phases will generate this dynamically from j2html code.
     */
    private String getStaticHtml() {
        return """
            <html>
            <head>
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
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
                    h1 { color: #2c3e50; margin-top: 0; }
                    h2 { color: #34495e; }
                    .status { color: #27ae60; font-weight: bold; }
                </style>
            </head>
            <body>
                <h1>j2html Preview Plugin</h1>
                <div class="card">
                    <h2>Phase 1: Tool Window ✓</h2>
                    <p><span class="status">Status: Active</span></p>
                    <p>This is a static HTML preview demonstrating that the plugin infrastructure is working correctly.</p>
                </div>
                <div class="card">
                    <h2>Next Steps</h2>
                    <ul>
                        <li>Phase 2: Detect current Java file</li>
                        <li>Phase 3: Find j2html methods</li>
                        <li>Phase 4: Execute and render dynamically</li>
                    </ul>
                </div>
                <div class="card">
                    <h2>Example j2html Component</h2>
                    <p>In future phases, this preview will show rendered output from code like:</p>
                    <pre><code>public static ContainerTag userCard(User user) {
    return div().withClass("card",
        h2(user.getName()),
        p(user.getEmail())
    );
}</code></pre>
                </div>
            </body>
            </html>
            """;
    }
}
