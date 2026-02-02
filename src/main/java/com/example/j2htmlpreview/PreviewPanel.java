package com.example.j2htmlpreview;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Main UI panel for the j2html preview tool window.
 * Phase 2: Detects and displays the currently open file.
 */
public class PreviewPanel extends JPanel {
    private final Project project;
    private final JLabel currentFileLabel;
    private final JEditorPane htmlPreview;
    
    public PreviewPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        
        // Header showing current file
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("j2html Preview - Phase 2");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        currentFileLabel = new JLabel("No file selected");
        currentFileLabel.setFont(currentFileLabel.getFont().deriveFont(Font.ITALIC));
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(currentFileLabel, BorderLayout.CENTER);
        
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
    
    /**
     * Set up listener for file editor changes.
     * This is where the event-driven magic happens!
     */
    private void setupFileListener() {
        // Get the message bus - IntelliJ's pub/sub system
        project.getMessageBus()
            .connect()
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                    // This gets called whenever the user switches files
                    updateCurrentFile();
                }
            });
    }
    
    /**
     * Update the UI to show information about the currently selected file.
     */
    private void updateCurrentFile() {
        // Get the file editor manager - tracks which files are open
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        
        // Get the currently selected file (can be null if nothing is open)
        VirtualFile currentFile = editorManager.getSelectedFiles().length > 0 
            ? editorManager.getSelectedFiles()[0] 
            : null;
        
        if (currentFile != null) {
            // Update the label with file info
            currentFileLabel.setText("Current file: " + currentFile.getName() + 
                                   " (" + currentFile.getPath() + ")");
            
            // Update the preview HTML
            htmlPreview.setText(getFileInfoHtml(currentFile));
        } else {
            currentFileLabel.setText("No file selected");
            htmlPreview.setText(getInitialHtml());
        }
    }
    
    /**
     * Generate HTML showing information about the current file.
     */
    private String getFileInfoHtml(VirtualFile file) {
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
                    .info { color: #7f8c8d; }
                    .success { color: #27ae60; font-weight: bold; }
                    code { 
                        background: #f4f4f4; 
                        padding: 2px 6px; 
                        border-radius: 3px;
                        font-family: 'Courier New', monospace;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>File Detected! ✓</h2>
                    <p><span class="success">Phase 2 is working!</span></p>
                    <p><strong>Filename:</strong> <code>%s</code></p>
                    <p><strong>Extension:</strong> <code>%s</code></p>
                    <p><strong>Path:</strong> <code>%s</code></p>
                </div>
                <div class="card">
                    <h2>What's Happening</h2>
                    <p>The plugin is now listening for file selection changes using IntelliJ's <code>MessageBus</code> system.</p>
                    <p class="info">Try switching between different files in your editor to see this update!</p>
                </div>
                <div class="card">
                    <h2>Next: Phase 3</h2>
                    <p>We'll analyze this file's contents to find methods that return j2html types like <code>ContainerTag</code>.</p>
                </div>
            </body>
            </html>
            """.formatted(
                file.getName(),
                file.getExtension() != null ? file.getExtension() : "none",
                file.getPath()
            );
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
                    <p>Open a Java file to see file detection in action.</p>
                </div>
            </body>
            </html>
            """;
    }
}
