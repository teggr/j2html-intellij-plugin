package com.example.j2htmlpreview;

/**
 * Static utility class for HTML template generation.
 * Provides reusable HTML templates for various states of the preview panel.
 */
public class HtmlTemplates {
    
    private HtmlTemplates() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Returns the initial welcome page HTML.
     */
    public static String getInitialPage() {
        return """
            <div class="alert alert-info" role="alert">
                <h4 class="alert-heading">j2html Preview</h4>
                <p>Open a Java file containing j2html methods to preview them.</p>
                <hr>
                <p class="mb-0">Methods that return ContainerTag, DomContent, Tag, or other j2html types will be detected.</p>
            </div>
            """;
    }
    
    /**
     * Returns the browser unavailable page HTML.
     */
    public static String getBrowserUnavailablePage() {
        return """
            <div class="alert alert-warning" role="alert">
                <h4 class="alert-heading">Browser Not Available</h4>
                <p>JCEF (Chromium Embedded Framework) is not supported in this environment.</p>
                <p class="mb-0">Bootstrap features will be unavailable in this session.</p>
            </div>
            """;
    }
    
    /**
     * Returns an error page with the given error message.
     * The error message will be HTML-escaped to prevent injection.
     */
    public static String getErrorPage(String errorMessage) {
        return """
            <div class="alert alert-danger" role="alert">
                <h4 class="alert-heading">Error</h4>
                <pre style="white-space: pre-wrap; word-wrap: break-word;">%s</pre>
            </div>
            """.formatted(escapeHtml(errorMessage));
    }
    
    /**
     * Returns an info page with the given info message.
     */
    public static String getInfoPage(String infoMessage) {
        return """
            <div class="alert alert-info" role="alert">
                <h4 class="alert-heading">Info</h4>
                <p class="mb-0">%s</p>
            </div>
            """.formatted(escapeHtml(infoMessage));
    }
    
    /**
     * Returns HTML for when the file is not a Java file.
     */
    public static String getNotJavaFilePage() {
        return """
            <div class="alert alert-warning" role="alert">
                <h4 class="alert-heading">Not a Java File</h4>
                <p class="mb-0">The currently selected file is not a Java source file.</p>
            </div>
            """;
    }
    
    /**
     * Returns HTML for when no j2html methods are found in the file.
     */
    public static String getNoMethodsFoundPage() {
        return """
            <div class="alert alert-info" role="alert">
                <h4 class="alert-heading">No j2html Methods Found</h4>
                <p>This file doesn't contain any methods that return j2html types.</p>
                <p class="mb-0">Methods returning ContainerTag, DomContent, Tag, UnescapedText, or Text will be detected.</p>
            </div>
            """;
    }
    
    /**
     * Returns HTML for when a method requires arguments to be provided.
     */
    public static String getMethodRequiresArgumentsPage() {
        return """
            <div class="alert alert-warning" role="alert">
                <h4 class="alert-heading">⚙️ Method Requires Arguments</h4>
                <p>The selected method requires parameters to be provided.</p>
                <hr>
                <p class="mb-0">
                    <strong>Next steps:</strong>
                    <ol class="mb-0 mt-2">
                        <li>Set the arguments in the expression editor panel above</li>
                        <li>Click the <strong>▶</strong> (Compile and Preview) button to render the HTML</li>
                    </ol>
                </p>
            </div>
            """;
    }
    
    /**
     * Wraps the given HTML content in a Bootstrap 5.3.3 page template.
     * Includes Bootstrap CSS and JS from CDN for styling.
     */
    public static String wrapInBootstrap(String htmlContent) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>j2html Preview</title>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" 
                      rel="stylesheet" 
                      integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" 
                      crossorigin="anonymous">
                <style>
                    body {
                        padding: 20px;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    }
                </style>
            </head>
            <body>
                %s
                <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" 
                        integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" 
                        crossorigin="anonymous"></script>
            </body>
            </html>
            """.formatted(htmlContent);
    }
    
    /**
     * Escapes HTML special characters to prevent injection attacks.
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
