# JCEF Browser Size Fix

## Problem
The JCEF browser component was not properly sized, causing the internal component to think it has more space than is actually visible in the plugin view. This resulted in content being rendered outside the visible viewport.

## Root Cause
When creating a JCEF browser in a Java Swing application, the browser component needs to:
1. Have its initial size explicitly set
2. Listen for resize events and update the browser viewport accordingly
3. Properly propagate size changes from parent containers

Without these, the JCEF Canvas component doesn't know its actual display size and renders content assuming infinite space.

## Solution Implemented

### Changes Made to `PreviewPanel.java`

#### 1. Added Imports for Component Listeners
```java
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
```

#### 2. Set Initial Size on Browser Component
When creating the JCEF browser:
```java
displayArea = webViewComponent.getComponent();

// Set initial size for the browser component
displayArea.setPreferredSize(new Dimension(800, 600));
```

#### 3. Added Component Listener to Browser Component
Listens for resize events on the browser component itself:
```java
displayArea.addComponentListener(new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
        // Force the browser component to update its size
        Component component = e.getComponent();
        if (component != null && webViewComponent != null) {
            // Ensure the browser knows about the new size
            Dimension size = component.getSize();
            component.setSize(size);
            component.revalidate();
            component.repaint();
        }
    }
});
```

#### 4. Added Component Listener to Scroll Pane
Propagates resize events from the scroll pane to the browser:
```java
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
```

#### 5. Set Preferred Size on Scroll Pane
```java
JScrollPane scrollPane = new JScrollPane(displayArea);
scrollPane.setPreferredSize(new Dimension(800, 600)); // Set initial preferred size
```

## How It Works

1. **Initial Sizing**: When the browser component is first created, it's given an initial preferred size of 800x600. This ensures it has a starting viewport size.

2. **Direct Resize Handling**: The browser component listens for its own resize events. When resized, it explicitly calls `setSize()` to inform the native browser, then `revalidate()` and `repaint()` to trigger layout and rendering updates.

3. **Parent Container Resize Propagation**: The scroll pane listens for its own resize events. When the tool window is resized, the scroll pane updates the browser component's size to match the viewport, using `SwingUtilities.invokeLater()` to ensure the update happens on the EDT.

4. **Layout Updates**: Both `revalidate()` and `repaint()` are called to ensure:
   - The layout manager recalculates component positions (`revalidate()`)
   - The component is visually redrawn (`repaint()`)

## References

The solution is based on standard JCEF integration practices:

1. **Set Initial Size**: Ensures the browser has a known viewport from the start
2. **Handle Resize Events**: JCEF requires explicit notification when the container size changes
3. **Use Swing EDT**: Size updates must happen on the Event Dispatch Thread for thread safety

## Testing

To test the fix:

1. Build the plugin: `./gradlew buildPlugin`
2. Run in sandbox: `./gradlew runIde`
3. Open the j2html Preview tool window
4. Resize the tool window and verify that:
   - Content fits within the visible area
   - Content properly reflows when the window is resized
   - No content is rendered outside the viewport

## Alternative Approaches Considered

### Using CefWindowInfo (Not Applicable)
Some online suggestions mentioned setting size via `CefWindowInfo.SetAsChild()`, but this is only applicable when creating a raw CEF browser. JBCefBrowser is a higher-level IntelliJ wrapper that handles CEF initialization internally.

### Disabling Multi-threaded Message Loop (Not Needed)
Some sources suggested disabling `multi_threaded_message_loop`, but this is not exposed in JBCefBrowser and is handled automatically by the IntelliJ platform.

### Direct Canvas Access (Not Recommended)
While it's possible to access the underlying Canvas component, it's better to work through the Swing component API for compatibility with IntelliJ's UI framework.

## Notes

- The fix uses standard Java Swing component listeners, which are compatible with IntelliJ's UI framework
- All size updates use `SwingUtilities.invokeLater()` when triggered from non-EDT threads to ensure thread safety
- The solution works with both the modern JCEF browser and the fallback JEditorPane
