# Preview Pane Width Fix

## Issue
The rendered HTML in the preview pane was flowing offscreen and showing horizontal scrollbars.

## Root Cause
The Java Swing `JScrollPane` component was configured with default horizontal scrollbar behavior (`HORIZONTAL_SCROLLBAR_AS_NEEDED`), which allowed horizontal scrolling when content exceeded the viewport width. The component-level constraints were not properly enforcing width limits.

## Solution Implemented

### Primary Fix: Java Component Constraints

#### 1. Disabled Horizontal Scrollbar
**File:** `PreviewPanel.java` (lines ~150-153)

Changed from:
```java
JScrollPane scrollPane = new JScrollPane(displayArea);
```

To:
```java
JScrollPane scrollPane = new JScrollPane(displayArea,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
```

**Impact:** Forces the JCEF browser component to constrain its rendering width to the available viewport width. No horizontal scrollbar can appear at the Java component level.

#### 2. Added Scroll Pane Size Constraints
**File:** `PreviewPanel.java` (lines ~155-157)

```java
scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
scrollPane.getViewport().setPreferredSize(null); // Let it size naturally
```

**Impact:** Ensures the scroll pane respects the container's width and doesn't try to expand beyond it.

#### 3. Set Minimum Panel Size
**File:** `PreviewPanel.java` (line ~84)

```java
setMinimumSize(new Dimension(100, 100));
```

**Impact:** Ensures the PreviewPanel respects container width constraints and properly resizes with the tool window.

### Secondary Fix: HTML/CSS Constraints

These changes provide defense-in-depth and ensure proper rendering even if content tries to overflow.

### Changes to `PreviewPanel.java` - `constructBootstrapPage()` method

#### 1. Updated Viewport Meta Tag
```html
<meta name='viewport' content='width=device-width, initial-scale=1, shrink-to-fit=no'>
```
- Added `shrink-to-fit=no` to prevent automatic shrinking on mobile-like viewports

#### 2. Comprehensive CSS Rules Added

**HTML & Body Level:**
```css
html { overflow-x: hidden; }
body { overflow-x: hidden !important; margin: 0; padding: 0; width: 100%; }
```
- Prevents horizontal scrolling at the root level
- Uses `!important` to override any Bootstrap styles

**Preview Container:**
```css
.preview-container { 
  width: 100%; 
  max-width: 100vw; 
  overflow-x: hidden !important; 
  overflow-wrap: break-word; 
  word-wrap: break-word; 
  box-sizing: border-box; 
  padding: 1rem; 
}
```
- Constrains container to viewport width (`100vw`)
- Forces word wrapping for long text
- Adds padding for breathing room

**All Child Elements:**
```css
.preview-container * { 
  max-width: 100%; 
  box-sizing: border-box; 
}
```
- Ensures no child element can exceed container width
- Includes padding/borders in width calculations

**Special Elements (images, tables, code):**
```css
.preview-container img, 
.preview-container table, 
.preview-container pre, 
.preview-container code { 
  max-width: 100%; 
  overflow-x: auto; 
}
```
- Constrains media and code blocks
- Allows internal scrolling for wide code/tables only

**Bootstrap Container Classes:**
```css
.container, .container-fluid, .container-sm, .container-md, .container-lg, .container-xl, .container-xxl { 
  max-width: 100% !important; 
  overflow-x: hidden !important; 
}
```
- Overrides all Bootstrap container widths
- Prevents Bootstrap's default breakpoint behavior

**Bootstrap Row Classes:**
```css
.row { 
  margin-left: 0 !important; 
  margin-right: 0 !important; 
  max-width: 100%; 
}
```
- Removes negative margins that Bootstrap rows use
- These negative margins can cause horizontal overflow

**Bootstrap Column Classes:**
```css
[class*='col-'] { 
  padding-left: 0.5rem; 
  padding-right: 0.5rem; 
}
```
- Reduces column padding to prevent overflow
- Applies to all Bootstrap column classes (col-*, col-sm-*, etc.)

#### 3. Removed Bootstrap Classes from Main Container
Changed from:
```html
<div class='preview-container container-fluid p-3'>
```

To:
```html
<div class='preview-container'>
```

- Removed `container-fluid` to avoid Bootstrap width conflicts
- Removed `p-3` class as padding is now handled in custom CSS
- Relies entirely on custom `.preview-container` styling

## Testing Instructions

1. Build the plugin (resolve Gradle/Java compatibility issues first)
2. Run the plugin in IntelliJ IDEA
3. Open a j2html component file (e.g., `test-files/ExampleJ2HtmlComponents.java`)
4. Select a method from the dropdown
5. Verify that:
   - No horizontal scrollbars appear
   - Content wraps within the preview pane
   - Long text breaks properly
   - Wide elements (tables, code blocks) are constrained
   - Bootstrap components render correctly without overflow

## Expected Behavior

- **No horizontal scrollbars** on the preview pane body
- Content stays within the **visible width** of the preview panel
- Long words and URLs **break/wrap** instead of extending beyond the edge
- Wide tables or code blocks may show **internal scrollbars** if needed
- Bootstrap grid layouts (rows/columns) **respect the width constraint**

## Files Modified

- `src/main/java/com/example/j2htmlpreview/PreviewPanel.java`
  - **Line ~84:** Added `setMinimumSize()` to PreviewPanel constructor
  - **Lines ~150-157:** Changed JScrollPane configuration to disable horizontal scrollbar and set size constraints
  - **Lines ~1470-1535:** Modified `constructBootstrapPage()` method with comprehensive CSS constraints

## Key Changes Summary

1. **Java Component Level (Primary Fix):**
   - `JScrollPane` configured with `HORIZONTAL_SCROLLBAR_NEVER`
   - Added size constraints to scroll pane viewport
   - Set minimum size on PreviewPanel

2. **HTML/CSS Level (Defense-in-Depth):**
   - Added viewport meta tag with `shrink-to-fit=no`
   - Applied `overflow-x: hidden !important` to html, body, and container
   - Override Bootstrap container widths with `max-width: 100% !important`
   - Remove negative margins from Bootstrap rows
   - Constrain all child elements with `max-width: 100%`

## Known Limitations

- If content contains extremely wide elements (like very wide tables or unbreakable strings), they may show internal scrollbars
- This is intentional and preferable to the entire page scrolling horizontally

## Future Improvements

- Consider adding a toggle to switch between "constrained" and "full-width" modes
- Add responsive breakpoints for different preview pane widths
- Consider making padding and margins configurable via settings
