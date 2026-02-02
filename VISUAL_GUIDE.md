# Phase 3 Visual Guide

## UI Layout

```
┌─────────────────────────────────────────────────────────────┐
│  j2html Preview - Phase 3                  [Tool Window]    │
│─────────────────────────────────────────────────────────────│
│  Current file: ExampleJ2HtmlComponents.java                 │
│                                                              │
│  Select method: [userCard(String name, String...) ▼]        │
│─────────────────────────────────────────────────────────────│
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ Method Selected ✓                                   │    │
│  │                                                      │    │
│  │ Phase 3 is working!                                 │    │
│  │                                                      │    │
│  │ Method: userCard                                    │    │
│  │ Return Type: ContainerTag                           │    │
│  │ Parameters: 2                                       │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ What's Happening                                    │    │
│  │                                                      │    │
│  │ We're using IntelliJ's PSI (Program Structure       │    │
│  │ Interface) to parse your Java code and find         │    │
│  │ methods that return j2html types.                   │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ Next: Phase 4                                       │    │
│  │                                                      │    │
│  │ We'll execute this method and render the actual     │    │
│  │ HTML output!                                        │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Example Method Signatures in Dropdown

When opening `ExampleJ2HtmlComponents.java`, the dropdown shows:

```
Select method: ▼
├─ simpleComponent() → ContainerTag
├─ userCard(String name, String email) → ContainerTag
├─ loginForm() → DomContent
├─ container(DomContent... content) → DivTag
└─ productCard(String name, double price, String imageUrl, boolean inStock) → ContainerTag
```

Note: `getTitle()` and `printMessage()` are NOT shown because they don't return j2html types.

## State Transitions

### State 1: No File Selected
```
┌────────────────────────────────────────┐
│ Waiting for file selection...          │
│                                         │
│ Open a Java file containing j2html     │
│ methods to see them listed here.       │
└────────────────────────────────────────┘

Dropdown: [No j2html methods found] (disabled)
```

### State 2: Non-Java File Selected
```
┌────────────────────────────────────────┐
│ Not a Java file                        │
│                                         │
│ The j2html preview only works with     │
│ Java files.                             │
└────────────────────────────────────────┘

Dropdown: [No j2html methods found] (disabled)
```

### State 3: Java File with No j2html Methods
```
┌────────────────────────────────────────┐
│ No j2html methods found                │
│                                         │
│ This file doesn't contain any methods  │
│ returning j2html types like            │
│ ContainerTag, DomContent, Tag, etc.    │
│                                         │
│ Try opening a file that contains       │
│ j2html component methods!              │
└────────────────────────────────────────┘

Dropdown: [No j2html methods found] (disabled)
```

### State 4: Java File with j2html Methods (before selection)
```
┌────────────────────────────────────────┐
│ j2html Methods Found! ✓                │
│                                         │
│ Found 5 method(s) returning j2html     │
│ types                                   │
│                                         │
│ Select a method from the dropdown      │
│ above to see details.                  │
└────────────────────────────────────────┘

Dropdown: [userCard(String name, String email) → ContainerTag] (enabled)
          [Other methods available...]
```

### State 5: Method Selected
```
┌────────────────────────────────────────┐
│ Method Selected ✓                      │
│                                         │
│ Phase 3 is working!                    │
│                                         │
│ Method: userCard                       │
│ Return Type: ContainerTag              │
│ Parameters: 2                           │
└────────────────────────────────────────┘

+ Additional cards explaining PSI and next steps

Dropdown: [userCard(String name, String email) → ContainerTag] (selected)
```

## PSI Detection Flow

```
User opens Java file
        ↓
FileEditorManagerListener triggers
        ↓
updateCurrentFile() called
        ↓
analyzeFile(VirtualFile) executed
        ↓
┌─────────────────────────────────────┐
│ PSI Navigation:                      │
│                                      │
│ VirtualFile                          │
│    ↓ PsiManager                      │
│ PsiFile                              │
│    ↓ Cast to PsiJavaFile             │
│ PsiJavaFile                          │
│    ↓ getClasses()                    │
│ PsiClass[]                           │
│    ↓ getMethods()                    │
│ PsiMethod[]                          │
│    ↓ filter: isJ2HtmlMethod()        │
│ List<PsiMethod> j2htmlMethods        │
└─────────────────────────────────────┘
        ↓
updateMethodSelector() populates dropdown
        ↓
Preview HTML updated based on findings
```

## Method Detection Examples

### ✅ Detected (j2html methods)
```java
// Returns ContainerTag
public static ContainerTag userCard(String name) { ... }

// Returns DomContent
public static DomContent loginForm() { ... }

// Returns DivTag (specific tag type)
public static DivTag container() { ... }

// Returns custom type with "Tag" suffix
public static CustomTag myComponent() { ... }
```

### ❌ Not Detected (non-j2html methods)
```java
// Returns String
public static String getTitle() { ... }

// Returns void
public static void printMessage(String msg) { ... }

// Returns primitive
public static int getCount() { ... }

// Returns non-tag object
public static User getUser() { ... }
```

## Key Implementation Details

### Method Signature Format
```
methodName(Type param1, Type param2) → ReturnType
```

Examples:
- `simpleComponent() → ContainerTag`
- `userCard(String name, String email) → ContainerTag`
- `container(DomContent... content) → DivTag`

### j2html Type Detection
The plugin identifies methods returning these types:
1. **Exact matches:**
   - ContainerTag
   - DomContent
   - Tag
   - DivTag
   - SpanTag
   - HtmlTag

2. **Pattern match:**
   - Any type containing "Tag" (e.g., CustomTag, FormTag, etc.)

### Event-Driven Updates
- File change detection: MessageBus subscription
- Automatic re-analysis on file switch
- No manual refresh required
- Real-time dropdown updates

## Testing the Implementation

### Manual Test Steps

1. **Build the plugin:**
   ```bash
   ./gradlew buildPlugin
   ```

2. **Run in IntelliJ sandbox:**
   ```bash
   ./gradlew runIde
   ```

3. **Open the test file:**
   - In the sandbox IDE, open `test-files/ExampleJ2HtmlComponents.java`

4. **Open the tool window:**
   - View → Tool Windows → j2html Preview (usually on the right side)

5. **Verify behavior:**
   - See 5 methods in dropdown
   - Select each method, verify details display
   - Switch to a file without j2html methods
   - Verify "No j2html methods found" message
   - Switch to a non-Java file
   - Verify "Not a Java file" message

### Expected Results
- ✅ Dropdown shows all 5 j2html methods from ExampleJ2HtmlComponents.java
- ✅ Each method signature is correctly formatted
- ✅ Selecting a method shows its details
- ✅ Switching files updates the UI automatically
- ✅ Non-Java files show appropriate message
- ✅ Files without j2html methods show appropriate message
