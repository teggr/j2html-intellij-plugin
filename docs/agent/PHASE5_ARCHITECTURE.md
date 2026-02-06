# Phase 5 Architecture Diagram

## Component Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    PreviewPanel UI                          │
├─────────────────────────────────────────────────────────────┤
│  Header                                                     │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ File: ExampleJ2HtmlComponents.java                    │ │
│  │ Method: [userCard(String, String) ▼]                  │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  Expression Evaluator (New in Phase 5)                     │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Quick test (write method call):                       │ │
│  │ ┌─────────────────────────────────────────────────┐   │ │
│  │ │ userCard("", "")          ← EditorTextField    │   │ │
│  │ │                             with Java syntax    │   │ │
│  │ │                             highlighting        │   │ │
│  │ └─────────────────────────────────────────────────┘   │ │
│  │ [Compile and Preview] ← Button                        │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  HTML Preview                                               │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ ℹ Status                                              │ │
│  │ Method has parameters. Write the method call...       │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Method Call Flow

```
User selects method from dropdown
         ↓
    onMethodSelected()
         ↓
    Has parameters? ──No──→ compileAndExecute() [Phase 4]
         │                        ↓
        Yes                  Execute directly
         ↓
populateExpressionEditor()
         ↓
buildMethodCallTemplate()
         ↓
getDefaultValueForType()
         ↓
Display template in editor
         ↓
    User edits code
         ↓
User clicks "Compile and Preview"
         ↓
    executeExpression()
         ↓
Create PsiExpressionCodeFragment
         ↓
CompilerManager.make(module)
         ↓
evaluateAndDisplay()
         ↓
evaluateExpressionReflection()
         ↓
[Currently throws exception - Phase 5 foundation]
         ↓
[Future: JavaCompiler API → Load class → Invoke → Render]
```

## Class Relationships

```
┌──────────────────────────────────────────────────────┐
│              PreviewPanel                            │
├──────────────────────────────────────────────────────┤
│ Fields:                                              │
│  - project: Project                                  │
│  - methodSelector: JComboBox<String>                 │
│  - htmlPreview: JEditorPane                          │
│  - j2htmlMethods: List<PsiMethod>                    │
│  ┌────────────────────────────────────────┐          │
│  │ New in Phase 5:                        │          │
│  │  - expressionEditor: EditorTextField   │          │
│  │  - currentFragment: PsiExpressionCF    │          │
│  │  - currentMethod: PsiMethod            │          │
│  └────────────────────────────────────────┘          │
│                                                      │
│ Methods (Phase 5 additions):                         │
│  + createExpressionEditor(): EditorTextField         │
│  + populateExpressionEditor(method)                  │
│  + buildMethodCallTemplate(method): String           │
│  + getDefaultValueForType(type): String              │
│  + executeExpression()                               │
│  + evaluateAndDisplay(fragment, module)              │
│  + evaluateExpressionReflection(text, module): Obj   │
│  + generateWrapperClass(...): String                 │
└──────────────────────────────────────────────────────┘
         │
         │ Uses
         ↓
┌──────────────────────────────────────────────────────┐
│        IntelliJ Platform APIs                        │
├──────────────────────────────────────────────────────┤
│ EditorFactory                                        │
│  └─ Creates Document instances                       │
│                                                      │
│ EditorTextField                                      │
│  └─ Mini code editor component                       │
│                                                      │
│ JavaFileType                                         │
│  └─ Provides Java syntax highlighting                │
│                                                      │
│ JavaCodeFragmentFactory                              │
│  └─ Creates PsiExpressionCodeFragment                │
│                                                      │
│ PsiExpressionCodeFragment                            │
│  └─ Represents editable Java expression              │
│                                                      │
│ PsiDocumentManager                                   │
│  └─ Synchronizes PSI ↔ Document                      │
└──────────────────────────────────────────────────────┘
```

## Data Flow: Template Generation

```
PsiMethod: userCard(String name, String email)
    ↓
buildMethodCallTemplate()
    ↓
For each parameter:
    ↓
    PsiParameter[0]: String name
        ↓
    getDefaultValueForType(String)
        ↓
    Returns: "\"\""
    ↓
    PsiParameter[1]: String email
        ↓
    getDefaultValueForType(String)
        ↓
    Returns: "\"\""
    ↓
Assemble: userCard("", "")
    ↓
Create PsiExpressionCodeFragment with template
    ↓
Get Document from fragment
    ↓
Update EditorTextField with document
    ↓
User sees: userCard("", "")
```

## Type Default Mapping

```
┌──────────────┬─────────────────────┐
│ Java Type    │ Default Value       │
├──────────────┼─────────────────────┤
│ String       │ ""                  │
│ int/Integer  │ 0                   │
│ long/Long    │ 0L                  │
│ boolean/...  │ false               │
│ double/...   │ 0.0                 │
│ List         │ List.of()           │
│ Custom Class │ new ClassName()     │
│ Unknown      │ null                │
└──────────────┴─────────────────────┘
```

## UI State Machine

```
       ┌────────────────┐
       │  No File Open  │
       └────────┬───────┘
                │ User opens Java file
                ↓
       ┌────────────────┐
       │  File Analyzed │
       └────────┬───────┘
                │ Methods found
                ↓
       ┌────────────────────┐
       │  Methods Listed    │
       └────────┬───────────┘
                │ User selects method
                ↓
         ┌──────┴──────┐
         │             │
    Zero params    Has params
         │             │
         ↓             ↓
  ┌───────────┐  ┌──────────────────┐
  │ Execute   │  │ Show Expression  │
  │ (Phase 4) │  │ Editor (Phase 5) │
  └───────────┘  └──────┬───────────┘
         │              │ User edits
         │              │ User clicks button
         │              ↓
         │       ┌──────────────┐
         │       │  Compiling   │
         │       └──────┬───────┘
         │              │
         ↓              ↓
       ┌────────────────────┐
       │  Show Result or    │
       │  Error Message     │
       └────────────────────┘
```

## Key Classes & Their Roles

### EditorTextField
- Embeds mini IntelliJ editor in Swing UI
- Provides syntax highlighting
- Enables code completion (Ctrl+Space)
- Supports multi-line editing

### PsiExpressionCodeFragment
- Represents editable Java expression
- Knows context (visible imports, classes)
- Knows expected return type
- Resolves references during editing

### JavaCodeFragmentFactory
- Creates code fragments
- Sets up proper context
- Configures fragment properties

### Document
- Backing data model for editor
- Synchronized with PSI by PsiDocumentManager
- Changes trigger PSI updates

## Future Enhancement Points

```
Phase 5b: Full Expression Evaluation
├─ Add JavaCompiler API integration
├─ Compile wrapper class at runtime
├─ Load compiled class dynamically
└─ Execute and render result

Phase 5c: Preview Method Generation
├─ Add "Save as preview" button
├─ Generate @J2HtmlPreview method
├─ Insert into test file
└─ Enable reuse across sessions

Phase 6: Live Updates
├─ Watch for code changes
├─ Re-execute automatically
├─ Background threading
└─ Progress indicators
```
