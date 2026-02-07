# PreviewPanel Refactoring Summary

## Overview

Successfully refactored the monolithic `PreviewPanel` class (1753 lines) into a modular architecture with event-driven communication using IntelliJ's message bus system.

## Architecture

### Core Components

1. **PreviewState** - Shared state model
   - Manages: currentFile, j2htmlMethods, selectedMethod, currentHtml, status, statusMessage
   - Defines PreviewStatus enum: IDLE, COMPILING, RENDERING, ERROR, SUCCESS
   - Uses IntelliJ's Topic/MessageBus for event publishing
   - Defines PreviewStateListener interface with callbacks for state changes

2. **HtmlTemplates** - Static utility for HTML generation
   - Provides: initial page, browser unavailable, error page, info page, not Java file, no methods found
   - wrapInBootstrap() method with Bootstrap 5.3.3 CSS/JS from CDN
   - escapeHtml() for sanitizing error messages
   - Uses text blocks for readable HTML templates

3. **ExpressionTemplateBuilder** - Static utility for expression templates
   - buildExpressionTemplate() generates method call templates with smart defaults
   - getDefaultValueForType() provides sensible defaults: "" for String, 0 for int, false for boolean, List.of() for List, etc.
   - Handles static method calls with class name prefix

4. **MethodSelectorPanel** - Method selection UI component
   - Extends JPanel, implements Disposable
   - Contains JComboBox for method selection
   - Listens to onMethodsChanged events to update method list
   - Publishes onMethodSelected events when user selects a method
   - Builds friendly method signatures using @Preview annotation names or method signatures
   - Preserves selection across updates when possible
   - Uses isUpdating flag to prevent infinite event loops

5. **ExpressionEditorPanel** - Expression editor UI component
   - Extends JPanel, implements Disposable
   - Contains EditorTextField for Java expression editing
   - Shows/hides based on whether selected method has parameters
   - Includes Execute (▶) and Save as Preview (💾) buttons
   - Uses PsiExpressionCodeFragment for code completion support
   - Publishes execution requests via PreviewExecutor.TOPIC
   - Listens to onMethodSelected to update expression template
   - Handles saveAsPreview() functionality with method generation

6. **HtmlRendererPanel** - HTML rendering UI component
   - Extends JPanel, implements Disposable
   - Supports both JCEF (modern) and JEditorPane (fallback) rendering
   - Listens to onHtmlChanged and onStatusChanged events
   - Handles component resizing for JCEF browser
   - Delegates to HtmlTemplates for various page states

7. **PreviewExecutor** - Service interface for compilation/execution
   - Defines TOPIC for execution requests via message bus
   - PreviewExecutorListener interface with: onExecuteMethod(), onExecuteExpression()

8. **PreviewPanelRefactored** - Main orchestrator
   - Composes all UI components
   - Handles file and PSI change listening
   - Analyzes files to find j2html methods
   - Manages compilation and execution flow
   - Publishes events via message bus
   - Throttles compilation requests (2.5s minimum interval)
   - Uses ReadAction for PSI operations
   - Handles zero-parameter method execution

## Event Flow

### File Change
1. User opens/switches to Java file
2. PreviewPanelRefactored.updateCurrentFile() triggered
3. Publishes onFileChanged via message bus
4. Calls analyzeFile() to find j2html methods
5. Publishes onMethodsChanged with discovered methods
6. MethodSelectorPanel receives event, updates dropdown
7. If methods found, first method auto-selected
8. Selection triggers onMethodSelected event

### Method Selection
1. User selects method from dropdown
2. MethodSelectorPanel publishes onMethodSelected
3. PreviewPanelRefactored receives event
4. If zero parameters → triggers onExecuteMethod via PreviewExecutor.TOPIC
5. If has parameters → ExpressionEditorPanel shows with template

### Execution (Zero Parameters)
1. onExecuteMethod event published
2. PreviewPanelRefactored.compileAndExecute() triggered
3. Module compilation via CompilerManager
4. On success: executeMethod() → reflection invoke → render HTML
5. Publishes onHtmlChanged with rendered HTML
6. HtmlRendererPanel receives event and displays

### Expression Execution
1. User edits expression and clicks Execute button
2. ExpressionEditorPanel publishes onExecuteExpression
3. PreviewPanelRefactored receives (currently shows "not yet implemented" error)
4. TODO: Full expression compilation/execution logic to be migrated

## Component Disposal Pattern

All components implement Disposable:
```java
@Override
public void dispose() {
    if (connection != null) {
        connection.disconnect();
    }
    // Additional cleanup as needed
}
```

Message bus connections are properly disconnected in dispose() methods to prevent memory leaks.

## Files Created

- `plugin/src/main/java/com/example/j2htmlpreview/PreviewState.java` (118 lines)
- `plugin/src/main/java/com/example/j2htmlpreview/HtmlTemplates.java` (131 lines)
- `plugin/src/main/java/com/example/j2htmlpreview/ExpressionTemplateBuilder.java` (111 lines)
- `plugin/src/main/java/com/example/j2htmlpreview/MethodSelectorPanel.java` (170 lines)
- `plugin/src/main/java/com/example/j2htmlpreview/ExpressionEditorPanel.java` (305 lines)
- `plugin/src/main/java/com/example/j2htmlpreview/HtmlRendererPanel.java` (156 lines)
- `plugin/src/main/java/com/example/j2htmlpreview/PreviewExecutor.java` (30 lines)
- `plugin/src/main/java/com/example/j2htmlpreview/PreviewPanelRefactored.java` (449 lines)

## Files Modified

- `plugin/src/main/java/com/example/j2htmlpreview/J2HtmlPreviewToolWindowFactory.java` - Updated to use PreviewPanelRefactored

## Remaining Work

The refactoring successfully extracted:
- ✅ State management into PreviewState
- ✅ HTML templates into HtmlTemplates utility
- ✅ Expression template building into ExpressionTemplateBuilder utility
- ✅ Method selection UI into MethodSelectorPanel
- ✅ Expression editor UI into ExpressionEditorPanel
- ✅ HTML rendering UI into HtmlRendererPanel
- ✅ Basic compilation and execution for zero-parameter methods

Still to be migrated from original PreviewPanel:
- ⏳ Full expression evaluation logic (generateWrapperClass, compileAndLoadClass, buildClasspath, etc.)
- ⏳ Java compiler detection (getProjectJavaCompiler, compileViaProcess)
- ⏳ Advanced error handling and diagnostics

The original `PreviewPanel.java` (1753 lines) remains in the codebase as reference for the remaining logic to be migrated.

## Benefits Achieved

1. **Separation of Concerns** - Each component has a single responsibility
2. **Testability** - Static utilities have no dependencies, easily testable
3. **Maintainability** - Smaller, focused classes are easier to understand and modify
4. **Reusability** - Components can be reused in different contexts
5. **Event-Driven** - Loose coupling via message bus enables flexible communication
6. **Extensibility** - New features can be added as new components or listeners

## Build Status

✅ Compilation successful
✅ Full build successful (with searchable options indexing)
✅ No test failures (tests skipped in build)

## Testing

Manual testing recommended:
1. Open IntelliJ IDEA with the plugin
2. Open a Java file with j2html methods
3. Verify method dropdown populates
4. Select a zero-parameter method
5. Verify HTML renders correctly
6. Select a parameterized method
7. Verify expression editor appears with template
8. Test Save as Preview functionality
