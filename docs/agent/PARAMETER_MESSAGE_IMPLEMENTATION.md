# Implementation: Preview Panel Message Enhancement

## Problem Statement
When changing the dropdown from a static method with arguments to one without (or vice versa), where the compile button needs to be pressed, the previous selection's HTML is still rendered in the preview panel. Users need a helpful message to indicate that the method requires arguments which can be set in the panel above, and then the compile button should be pressed.

## Solution Overview
The solution adds a helpful message to the preview panel when a method requiring parameters is selected. This message replaces the previous HTML output and provides clear instructions to the user.

## Changes Made

### 1. HtmlTemplates.java
Added a new template method `getMethodRequiresArgumentsPage()` that returns a Bootstrap-styled warning message:
- Uses `alert-warning` class for appropriate visual styling
- Includes a gear emoji (⚙️) to indicate configuration is needed
- Provides step-by-step instructions:
  1. Set the arguments in the expression editor panel above
  2. Click the ▶ (Compile and Preview) button to render the HTML

### 2. PreviewPanel.java

#### New Method: `showMethodRequiresArgumentsMessage()`
- Displays the method-requires-arguments message
- Supports both JCEF (modern browser) and JEditorPane (legacy) rendering modes
- Uses consistent styling with other messages in the application
- For JCEF: Wraps the message in the Bootstrap page template
- For legacy: Provides a simple HTML fallback with appropriate styling

#### Updated Method: `onMethodSelected()`
- When a parameterized method is selected (parameters count > 0):
  - Shows the evaluator panel
  - Populates the expression editor with a template
  - **NEW**: Calls `showMethodRequiresArgumentsMessage()` to display the helpful message

## User Experience Flow

### Before (Problem):
1. User has method A (no parameters) selected → HTML is displayed
2. User switches to method B (has parameters) → Previous HTML from method A is still visible
3. User is confused why nothing changed

### After (Solution):
1. User has method A (no parameters) selected → HTML is displayed
2. User switches to method B (has parameters) → Clear message is displayed explaining:
   - The method requires arguments
   - How to set the arguments in the editor above
   - How to compile and preview by clicking the ▶ button
3. User follows the instructions and successfully previews method B

## Technical Details

- **Minimal Changes**: Only 2 files modified with focused changes
- **Consistent Styling**: Uses existing HtmlTemplates pattern and Bootstrap styling
- **Backward Compatible**: Works with both JCEF and legacy JEditorPane
- **No Breaking Changes**: Existing functionality remains unchanged
- **Build Status**: ✅ All builds successful
- **Security**: ✅ No vulnerabilities detected by CodeQL

## Testing Considerations

Since this is an IntelliJ plugin requiring UI interaction:
- Manual testing in IntelliJ IDE is recommended
- Test scenarios:
  1. Switch from no-param method to param method
  2. Switch from param method to no-param method
  3. Switch between different param methods
  4. Verify message displays correctly in both JCEF and legacy modes

## Files Modified
- `plugin/src/main/java/com/example/j2htmlpreview/HtmlTemplates.java`
- `plugin/src/main/java/com/example/j2htmlpreview/PreviewPanel.java`
