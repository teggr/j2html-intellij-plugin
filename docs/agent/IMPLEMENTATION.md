# PreviewPanel Refactoring Implementation Guide

## Architecture Overview

The refactored architecture follows an event-driven, component-based design using IntelliJ's message bus system.

```
┌─────────────────────────────────────────────────────────────┐
│                 PreviewPanelRefactored                       │
│                   (Orchestrator)                             │
│                                                              │
│  • File/PSI change listening                                │
│  • Method analysis                                          │
│  • Compilation/execution coordination                       │
│  • Event publishing                                         │
└───────┬─────────────────────────────────────────────────────┘
        │
        │ Message Bus (IntelliJ Topic System)
        │
        ├─────────────────────────────────────────────────────┐
        │                                                      │
        ▼                                                      ▼
┌───────────────┐                                    ┌──────────────────┐
│ PreviewState  │ ◄──────────────────────────────── │ PreviewExecutor  │
│   (Model)     │                                    │   (Interface)    │
│               │                                    │                  │
│ • State data  │                                    │ • Execution      │
│ • Status enum │                                    │   requests       │
└───────────────┘                                    └──────────────────┘
        │
        │ Events: onFileChanged, onMethodsChanged, 
        │         onMethodSelected, onHtmlChanged,
        │         onStatusChanged
        │
        ├─────────────────┬─────────────────┬────────────────┐
        │                 │                 │                │
        ▼                 ▼                 ▼                ▼
┌─────────────┐  ┌─────────────────┐  ┌──────────┐  ┌──────────────┐
│   Method    │  │   Expression    │  │   Html   │  │  Utilities   │
│  Selector   │  │     Editor      │  │ Renderer │  │              │
│   Panel     │  │     Panel       │  │  Panel   │  │ • Templates  │
│             │  │                 │  │          │  │ • Builders   │
│ • Method    │  │ • Expression    │  │ • JCEF/  │  │              │
│   dropdown  │  │   editor        │  │   Fallback│  │              │
│ • @Preview  │  │ • Execute btn   │  │ • Resize │  │              │
│   support   │  │ • Save btn      │  │   handling│  │              │
└─────────────┘  └─────────────────┘  └──────────┘  └──────────────┘
```

## Message Bus Topics

### PreviewState.TOPIC
**Purpose:** Broadcasts state changes to all subscribed components

**Events:**
- `onFileChanged(VirtualFile file)` - Current file changed
- `onMethodsChanged(List<PsiMethod> methods)` - j2html methods discovered
- `onMethodSelected(PsiMethod method)` - User selected a method
- `onHtmlChanged(String html)` - HTML content changed
- `onStatusChanged(PreviewStatus status, String message)` - Status update

### PreviewExecutor.TOPIC
**Purpose:** Handles execution requests

**Events:**
- `onExecuteMethod(PsiMethod method)` - Execute zero-parameter method
- `onExecuteExpression(PsiMethod contextMethod, String expression)` - Evaluate expression

## Component Communication Patterns

### 1. File Change Flow
```
User opens file
     ↓
FileEditorManagerListener
     ↓
PreviewPanelRefactored.updateCurrentFile()
     ↓
PreviewPanelRefactored.analyzeFile()
     ↓
Publish: onMethodsChanged(methods)
     ↓
MethodSelectorPanel receives & updates dropdown
```

### 2. Method Selection Flow
```
User selects method from dropdown
     ↓
MethodSelectorPanel.onMethodSelected()
     ↓
Publish: PreviewState.TOPIC.onMethodSelected(method)
     ↓
PreviewPanelRefactored receives
     ↓
If zero params: Publish PreviewExecutor.TOPIC.onExecuteMethod()
     ↓
ExpressionEditorPanel receives & shows/hides
```

### 3. Execution Flow
```
PreviewExecutor.TOPIC.onExecuteMethod()
     ↓
PreviewPanelRefactored.compileAndExecute()
     ↓
ReadAction.nonBlocking() → resolve module
     ↓
CompilerManager.make() → async compilation
     ↓
On success: executeMethod()
     ↓
Reflection invoke → render HTML
     ↓
Publish: onHtmlChanged(html)
     ↓
HtmlRendererPanel receives & displays
```

## Key Design Patterns

### 1. Publisher-Subscriber (Message Bus)
Components don't directly reference each other. They communicate via events.

```java
// Publishing
project.getMessageBus()
    .syncPublisher(PreviewState.TOPIC)
    .onMethodSelected(method);

// Subscribing
connection = project.getMessageBus().connect(this);
connection.subscribe(PreviewState.TOPIC, new PreviewState.PreviewStateListener() {
    @Override
    public void onMethodSelected(PsiMethod method) {
        // Handle event
    }
});
```

### 2. Disposable Pattern
All components implement Disposable for proper cleanup.

```java
@Override
public void dispose() {
    if (connection != null) {
        connection.disconnect();  // Disconnect message bus
    }
    // Additional cleanup
}
```

### 3. Static Utilities
HtmlTemplates and ExpressionTemplateBuilder are static utilities with no state or dependencies.

```java
// Pure functions - easily testable
String html = HtmlTemplates.getErrorPage("Error message");
String template = ExpressionTemplateBuilder.buildExpressionTemplate(method);
```

### 4. Update Flag Pattern
Prevents infinite loops during programmatic updates.

```java
private boolean isUpdating = false;

void updateUI() {
    isUpdating = true;
    try {
        // Update UI components
    } finally {
        isUpdating = false;
    }
}

void onUserAction() {
    if (!isUpdating) {
        // Process user action
    }
}
```

## Threading Considerations

### ReadAction for PSI Operations
PSI (Program Structure Interface) operations require read access.

```java
ReadAction.nonBlocking(() -> {
    // PSI operations here
    return data;
}).finishOnUiThread(ModalityState.defaultModalityState(), result -> {
    // UI updates here
}).submit(AppExecutorUtil.getAppExecutorService());
```

### SwingUtilities for UI Updates
Ensure UI updates happen on EDT (Event Dispatch Thread).

```java
SwingUtilities.invokeLater(() -> {
    // Update UI components
});
```

### Async Compilation
CompilerManager.make() is asynchronous - callback runs on background thread.

```java
CompilerManager.getInstance(project).make(module, new CompileStatusNotification() {
    @Override
    public void finished(boolean aborted, int errors, int warnings, CompileContext context) {
        SwingUtilities.invokeLater(() -> {
            // Handle compilation result on UI thread
        });
    }
});
```

## State Management

### PreviewState
Centralized state container (not actively used for storage yet, but defines the contract).

```java
// State fields
private VirtualFile currentFile;
private List<PsiMethod> j2htmlMethods;
private PsiMethod selectedMethod;
private String currentHtml;
private PreviewStatus status;
private String statusMessage;

// Package-private setters allow controlled mutation
void setCurrentFile(VirtualFile file) {
    this.currentFile = file;
}
```

Currently, PreviewPanelRefactored maintains its own state and publishes changes. Future enhancement: fully centralize state in PreviewState.

## Extension Points

### Adding a New UI Component

1. Create component class extending JPanel, implementing Disposable
2. Subscribe to PreviewState.TOPIC events in constructor
3. Store MessageBusConnection and disconnect in dispose()
4. Update UI in response to events
5. Publish user actions via message bus
6. Add to PreviewPanelRefactored layout

Example:
```java
public class MyNewPanel extends JPanel implements Disposable {
    private MessageBusConnection connection;
    
    public MyNewPanel(Project project) {
        connection = project.getMessageBus().connect(this);
        connection.subscribe(PreviewState.TOPIC, new PreviewState.PreviewStateListener() {
            @Override
            public void onSomeEvent() {
                // Update UI
            }
        });
    }
    
    @Override
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
        }
    }
}
```

### Adding a New Event

1. Add method to PreviewState.PreviewStateListener interface
2. Implement default method body (empty or no-op)
3. Publish event via syncPublisher()
4. Subscribe in components that need to handle it

Example:
```java
// In PreviewState.PreviewStateListener
default void onCompilationStarted(Module module) {}

// Publishing
project.getMessageBus()
    .syncPublisher(PreviewState.TOPIC)
    .onCompilationStarted(module);
```

## Testing Strategy

### Unit Tests for Static Utilities
```java
@Test
void testExpressionTemplateBuilder() {
    PsiMethod method = // ... create mock
    String template = ExpressionTemplateBuilder.buildExpressionTemplate(method);
    assertEquals("MyClass.myMethod(\"\", 0)", template);
}
```

### Component Tests
```java
@Test
void testMethodSelectorPanel() {
    MethodSelectorPanel panel = new MethodSelectorPanel(mockProject);
    // Publish onMethodsChanged event
    // Assert dropdown updated correctly
}
```

### Integration Tests
Test full event flow through multiple components using IntelliJ's test framework.

## Migration from Original PreviewPanel

### Completed
- Basic UI structure and layout
- File and PSI change listening
- Method analysis and discovery
- Method selection and display
- Zero-parameter method execution
- HTML rendering with JCEF/fallback
- Expression editor UI with Save as Preview

### Pending
- Expression compilation and evaluation
- Dynamic class generation (ExpressionWrapper)
- Custom compiler detection (getProjectJavaCompiler)
- Process-based compilation fallback
- Advanced error handling and diagnostics
- Full classpath building logic

### Migration Approach
1. Keep original PreviewPanel as reference
2. Gradually port logic from executeExpression() flow
3. Extract into PreviewExecutorService (not just interface)
4. Test incrementally
5. Once complete, remove original PreviewPanel

## Performance Considerations

### Debouncing
PSI changes are debounced (400ms) to avoid excessive analysis.

```java
psiChangeAlarm.cancelAllRequests();
psiChangeAlarm.addRequest(() -> analyzeFile(file), DEBOUNCE_DELAY_MS);
```

### Throttling
Compilations are throttled (2.5s minimum) to prevent overlapping builds.

```java
long currentTime = System.currentTimeMillis();
if (currentTime - lastCompilationTime < COMPILATION_THROTTLE_MS) {
    return;
}
lastCompilationTime = currentTime;
```

### Async Operations
Heavy operations (PSI analysis, compilation) run on background threads.

### Lazy Initialization
Components created only when tool window is opened.

## Security Considerations

### HTML Escaping
All user-provided or error messages are HTML-escaped before rendering.

```java
String escaped = HtmlTemplates.escapeHtml(userInput);
```

### Class Loading
Module-specific classloaders prevent classpath pollution.

### Reflection Safety
Methods are made accessible but invoked in controlled context.

## Next Steps

1. **Complete Expression Evaluation**
   - Port remaining logic from PreviewPanel
   - Create PreviewExecutorService implementation
   - Test expression compilation and execution

2. **Enhance Error Handling**
   - Better error messages
   - Diagnostic information
   - Recovery strategies

3. **Add Tests**
   - Unit tests for utilities
   - Integration tests for components
   - UI tests for full flow

4. **Documentation**
   - API documentation
   - User guide
   - Developer guide

5. **Remove Original PreviewPanel**
   - Once all functionality migrated
   - Verify no regressions
   - Clean up unused code
