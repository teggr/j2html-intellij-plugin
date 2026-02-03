# Phase 4 Architecture Diagram

## Component Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                  j2html Preview Tool Window                     │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  Header: "j2html Preview - Phase 4"                      │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  Dropdown: [buildMyDiv() ▼]   [buildMyParagraph()] etc.  │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                                                           │ │
│  │           HTML Preview Panel                              │ │
│  │                                                           │ │
│  │  Shows one of:                                            │ │
│  │  • Info: "Compiling..." (blue box)                        │ │
│  │  • Success: "✓ Rendered successfully" + HTML (green)      │ │
│  │  • Error: "⚠ Error: ..." (red box)                        │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Execution Flow

```
┌──────────────┐
│ User Action  │
│ Select method│
│ from dropdown│
└──────┬───────┘
       │
       ▼
┌─────────────────────┐
│ onMethodSelected()  │
│ (EDT Thread)        │
└──────┬──────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ compileAndExecute(psiMethod)     │
│ • Find module                    │
│ • showInfo("Compiling...")       │
└──────┬───────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ CompilerManager.make(module, ...)   │
│ (Async - Returns Immediately)       │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │  Background Compilation Thread  │ │
│ │  • Incremental compilation      │ │
│ │  • Only changed files           │ │
│ └─────┬───────────────────────────┘ │
└───────┼─────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────┐
│ callback.finished(aborted, errors,...) │
│ (Background Thread - NOT EDT!)         │
└────────┬───────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ SwingUtilities.invokeLater(() -> {   │
│   // Now on EDT - safe to update UI  │
│                                      │
│   if (aborted)                       │
│     showError("Aborted")             │
│   else if (errors > 0)               │
│     showError("Failed with X errors")│
│   else                               │
│     executeMethod(psiMethod)         │
│ })                                   │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────┐       ┌────────────────────┐
│ Success Path             │       │ Error Path         │
│ executeMethod()          │       │ showError()        │
│ • Load compiled class    │       │ • Display red box  │
│ • Invoke static method   │       │ • Show error msg   │
│ • Get HTML string        │       └────────────────────┘
│ • showSuccess(html)      │
└──────────────────────────┘
```

## Thread Safety Pattern

```
Thread Timeline:

EDT Thread          Background Thread           EDT Thread
────────────       ─────────────────           ────────────
User clicks    →   [not involved]           →  [not involved]
   │
   ▼
compileAndExecute()
showInfo("Compiling...")
   │
   ▼
CompilerManager.make()
   │                      │
   │                      ▼
   │              Compilation starts
   │              • javac running
   │              • .class files
   │                      │
   │                      ▼
   │              callback.finished()
   │                      │
   │                      ▼
   │              SwingUtilities.invokeLater()
   │                      │
   ▼                      ▼
[continues UI work] → [schedules work] →  executeMethod()
                                          showSuccess()
                                          UI updated safely
```

## Key Design Decisions

### 1. Module-Level Compilation
**Why:** Methods may depend on other classes in the module
**Result:** Compile entire module, not just single file
**Benefit:** IntelliJ's incremental compiler only recompiles changes

### 2. Asynchronous Compilation
**Why:** Large projects can take time to compile
**Result:** CompilerManager.make() returns immediately
**Benefit:** UI remains responsive during compilation

### 3. SwingUtilities.invokeLater()
**Why:** Compiler callbacks run on background threads
**Result:** Schedule UI updates on EDT
**Benefit:** Thread-safe, no UI corruption

### 4. Three-State UI
**Why:** Clear feedback for user at each stage
**Result:** Info (compiling) → Success/Error
**Benefit:** User knows what's happening

## Error Handling Matrix

```
┌────────────────────┬─────────────────────────────────────┐
│ Condition          │ Result                              │
├────────────────────┼─────────────────────────────────────┤
│ Compilation aborted│ "Compilation was aborted."          │
├────────────────────┼─────────────────────────────────────┤
│ Compilation errors │ "Compilation failed with X error(s).│
│                    │  Check the Problems panel..."       │
├────────────────────┼─────────────────────────────────────┤
│ Module not found   │ "Could not find module for class"   │
├────────────────────┼─────────────────────────────────────┤
│ Class not found    │ "Class not found. Make sure the     │
│                    │  project is compiled: ..."          │
├────────────────────┼─────────────────────────────────────┤
│ Method not found   │ "Method not found: ..."             │
├────────────────────┼─────────────────────────────────────┤
│ Execution exception│ "Error executing method: ..."       │
├────────────────────┼─────────────────────────────────────┤
│ Success            │ "✓ Rendered successfully" + HTML    │
└────────────────────┴─────────────────────────────────────┘
```

## Code Organization

```
PreviewPanel.java
├── Constructor
│   ├── Create UI components
│   ├── Setup method selector dropdown
│   └── Initialize HTML preview pane
│
├── UI Event Handlers
│   └── onMethodSelected() → compileAndExecute()
│
├── Compilation
│   └── compileAndExecute(PsiMethod)
│       ├── Find module
│       ├── Show "Compiling..."
│       └── CompilerManager.make(callback)
│
├── Execution
│   ├── executeMethod(PsiMethod)
│   ├── createModuleClassLoader(Module)
│   └── [reflection to invoke method]
│
├── Method Detection
│   ├── refreshMethods()
│   ├── isJ2HtmlMethod(PsiMethod)
│   └── getCurrentFile()
│
└── UI Display
    ├── showInfo(String) - blue info box
    ├── showError(String) - red error box
    └── showSuccess(String) - green success banner
```

## API Usage

### CompilerManager
```java
CompilerManager.getInstance(project)
    .make(module, new CompileStatusNotification() {
        @Override
        public void finished(boolean aborted, 
                           int errors, 
                           int warnings, 
                           CompileContext context) {
            // Handle completion
        }
    });
```

### Thread Safety
```java
SwingUtilities.invokeLater(() -> {
    // This runs on EDT - safe to update UI
    htmlPreview.setText(html);
});
```

### Module Detection
```java
Module module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
```
