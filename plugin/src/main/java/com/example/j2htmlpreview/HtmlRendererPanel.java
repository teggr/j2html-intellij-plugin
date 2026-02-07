package com.example.j2htmlpreview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.messages.MessageBusConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * UI component for rendering HTML content.
 * Supports both JCEF (modern Chromium browser) and JEditorPane (fallback) rendering.
 * Listens to state changes and updates the displayed HTML accordingly.
 */
public class HtmlRendererPanel extends JPanel implements Disposable {
    
    private final Project project;
    private final boolean hasModernBrowserSupport;
    private JBCefBrowser webViewComponent;
    private JEditorPane legacyHtmlPane;
    private MessageBusConnection connection;
    private JScrollPane scrollPane;
    
    public HtmlRendererPanel(Project project) {
        this.project = project;
        this.hasModernBrowserSupport = JBCefApp.isSupported();
        
        setLayout(new BorderLayout());
        
        JComponent displayArea = createDisplayComponent();
        scrollPane = new JScrollPane(displayArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        
        // Handle scroll pane resize to update browser viewport
        if (hasModernBrowserSupport) {
            final JComponent finalDisplayArea = displayArea;
            scrollPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    // Update the display area size when scroll pane is resized
                    if (finalDisplayArea != null && webViewComponent != null) {
                        SwingUtilities.invokeLater(() -> {
                            Dimension viewportSize = scrollPane.getViewport().getExtentSize();
                            finalDisplayArea.setPreferredSize(viewportSize);
                            finalDisplayArea.setSize(viewportSize);
                            finalDisplayArea.revalidate();
                            finalDisplayArea.repaint();
                        });
                    }
                }
            });
        }
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Subscribe to state changes
        setupStateListener();
        
        // Show initial page
        renderHtml(HtmlTemplates.getInitialPage());
    }
    
    private JComponent createDisplayComponent() {
        JComponent displayArea;
        
        if (hasModernBrowserSupport) {
            webViewComponent = new JBCefBrowser();
            displayArea = webViewComponent.getComponent();
            
            // Set initial size for the browser component
            displayArea.setPreferredSize(new Dimension(800, 600));
            
            // Handle resize events to update browser viewport
            // JCEF requires the Canvas component to be properly sized for correct rendering
            displayArea.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    // Force the browser component to update its size
                    Component component = e.getComponent();
                    if (component != null && webViewComponent != null) {
                        Dimension size = component.getSize();
                        component.setSize(size);
                        component.revalidate();
                        component.repaint();
                    }
                }
            });
        } else {
            // Fallback to JEditorPane
            String unavailableHtml = HtmlTemplates.wrapInBootstrap(
                HtmlTemplates.getBrowserUnavailablePage()
            );
            legacyHtmlPane = new JEditorPane("text/html", unavailableHtml);
            legacyHtmlPane.setEditable(false);
            displayArea = legacyHtmlPane;
        }
        
        return displayArea;
    }
    
    private void setupStateListener() {
        connection = project.getMessageBus().connect(this);
        connection.subscribe(PreviewState.TOPIC, new PreviewState.PreviewStateListener() {
            @Override
            public void onHtmlChanged(String html) {
                renderHtml(html);
            }
            
            @Override
            public void onStatusChanged(PreviewState.PreviewStatus status, String message) {
                if (status == PreviewState.PreviewStatus.ERROR) {
                    renderHtml(HtmlTemplates.getErrorPage(message));
                } else if (status == PreviewState.PreviewStatus.COMPILING) {
                    renderHtml(HtmlTemplates.getInfoPage("Compiling..."));
                }
            }
        });
    }
    
    /**
     * Renders HTML content to the display component.
     * Wraps content in Bootstrap template for JCEF browser.
     */
    public void renderHtml(String htmlContent) {
        SwingUtilities.invokeLater(() -> {
            if (hasModernBrowserSupport && webViewComponent != null) {
                String wrappedHtml = HtmlTemplates.wrapInBootstrap(htmlContent);
                webViewComponent.loadHTML(wrappedHtml);
            } else if (legacyHtmlPane != null) {
                legacyHtmlPane.setText(htmlContent);
                legacyHtmlPane.setCaretPosition(0);
            }
        });
    }
    
    @Override
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
        }
        if (webViewComponent != null && !webViewComponent.isDisposed()) {
            com.intellij.openapi.util.Disposer.dispose(webViewComponent);
        }
    }
}
