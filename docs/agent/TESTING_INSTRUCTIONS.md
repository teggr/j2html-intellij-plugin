# Testing Instructions for Preview Pane Width Fix

## Overview
The preview pane width fix has been implemented at the Java Swing component level. This is the correct approach because:
1. The JCEF browser component needs to be told to constrain its width
2. The JScrollPane was allowing horizontal scrolling
3. Java components needed proper size constraints

## Changes Made

### 1. JScrollPane Configuration (Primary Fix)
**Location:** `PreviewPanel.java` line ~150-153

The `JScrollPane` that wraps the browser component now explicitly disables horizontal scrolling:

```java
JScrollPane scrollPane = new JScrollPane(displayArea,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
```

**Effect:** This forces the browser component to render content within the available width. The horizontal scrollbar is physically disabled at the container level.

### 2. Scroll Pane Size Constraints
**Location:** `PreviewPanel.java` line ~155-157

```java
scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
scrollPane.getViewport().setPreferredSize(null);
```

**Effect:** Ensures the scroll pane doesn't try to expand beyond its container and sizes naturally to fill available space.

### 3. Panel Minimum Size
**Location:** `PreviewPanel.java` line ~84

```java
setMinimumSize(new Dimension(100, 100));
```

**Effect:** Ensures the PreviewPanel properly responds to container resizing.

## How to Test

### Setup
1. Resolve Gradle/Java compatibility issues if needed
2. Build the plugin: `.\gradlew buildPlugin`
3. Run the plugin: `.\gradlew runIde`
4. Open the test file: `test-files/ExampleJ2HtmlComponents.java`

### Test Cases

#### Test 1: Basic Width Constraint
1. Open `ExampleJ2HtmlComponents.java`
2. Select `simpleComponent()` from the dropdown
3. **Expected:** No horizontal scrollbar appears
4. **Verify:** Content stays within the preview pane width

#### Test 2: Wide Content
1. Create a test method with wide content:
```java
public static ContainerTag wideContent() {
    return div(
        h1("This is a very long heading that should wrap instead of causing horizontal scrolling"),
        p("Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
    );
}
```
2. Select this method
3. **Expected:** Text wraps within the pane, no horizontal scrollbar

#### Test 3: Resize Tool Window
1. With any method selected, drag the preview tool window to make it narrower
2. **Expected:** Content reflows to fit the new width
3. **Expected:** No horizontal scrollbar appears

#### Test 4: Bootstrap Grid Content
1. Create a test method with Bootstrap grid:
```java
public static ContainerTag gridContent() {
    return div().withClass("container").with(
        div().withClass("row").with(
            div().withClass("col-md-6").with(p("Column 1")),
            div().withClass("col-md-6").with(p("Column 2"))
        )
    );
}
```
2. Select this method
3. **Expected:** Grid columns stack or shrink to fit, no horizontal overflow

#### Test 5: Table Content
1. Create a test method with a table:
```java
public static ContainerTag tableContent() {
    return table().withClass("table").with(
        thead(tr(th("Header 1"), th("Header 2"), th("Header 3"))),
        tbody(
            tr(td("Data 1"), td("Data 2"), td("Data 3")),
            tr(td("Data 4"), td("Data 5"), td("Data 6"))
        )
    );
}
```
2. Select this method
3. **Expected:** Table fits within width or shows internal scrolling (not page-level scrolling)

## Success Criteria

✅ **No horizontal scrollbar** appears on the main preview pane body  
✅ Content **wraps and reflows** to fit available width  
✅ Tool window **resizing works** properly without breaking layout  
✅ Bootstrap components **render correctly** within width constraints  
✅ Long text and URLs **break/wrap** instead of overflowing  

## Debugging

If horizontal scrollbars still appear:

### Check 1: Verify JScrollPane Policy
Add debug logging to confirm the scroll pane policy:
```java
System.out.println("Horizontal policy: " + scrollPane.getHorizontalScrollBarPolicy());
// Should print: 31 (HORIZONTAL_SCROLLBAR_NEVER)
```

### Check 2: Inspect Browser Component Size
Add debug logging to see the browser component dimensions:
```java
displayArea.addComponentListener(new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
        System.out.println("Browser component size: " + e.getComponent().getSize());
    }
});
```

### Check 3: CSS Override Issues
If the problem persists, the issue might be with specific Bootstrap components. Check the browser's dev tools (if available in JCEF) to see which element is causing overflow.

### Check 4: JCEF-Specific Settings
The JCEF browser might have its own settings. Try adding this after creating the browser:
```java
webViewComponent.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        // Execute JavaScript to force width constraints
        browser.executeJavaScript(
            "document.body.style.overflow = 'hidden';", 
            browser.getURL(), 0
        );
    }
}, webViewComponent.getCefBrowser());
```

## Alternative Solutions (If Current Fix Doesn't Work)

### Option 1: Wrap Browser Component
```java
JPanel browserWrapper = new JPanel(new BorderLayout());
browserWrapper.add(displayArea, BorderLayout.CENTER);
browserWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
JScrollPane scrollPane = new JScrollPane(browserWrapper, ...);
```

### Option 2: Use Different Layout Manager
```java
setLayout(new GridBagLayout());
GridBagConstraints gbc = new GridBagConstraints();
gbc.fill = GridBagConstraints.BOTH;
gbc.weightx = 1.0;
gbc.weighty = 1.0;
add(scrollPane, gbc);
```

### Option 3: Override getPreferredScrollableViewportSize
Create a custom wrapper that overrides scrollable viewport size:
```java
class ConstrainedWidthPanel extends JPanel implements Scrollable {
    // Override getPreferredScrollableViewportSize to constrain width
}
```

## Expected Behavior Summary

With these changes, the preview pane should behave like a modern responsive web view:
- Content automatically fits the available width
- No horizontal scrolling at the container level
- Proper text wrapping and reflow
- Bootstrap components adapt to the constrained width
- Only very wide elements (like code blocks or wide tables) might show internal scrolling

The key insight is that **the Java Swing container controls the JCEF browser's rendering viewport**, so fixing it at that level is the proper solution rather than trying to fix it with CSS alone.
