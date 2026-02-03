# Phase 4 Execution Flow Diagram

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       User Interface (Swing)                     │
│  ┌────────────────┐    ┌──────────────────────────────────┐    │
│  │ Method Selector│───▶│    HTML Preview Pane             │    │
│  └────────────────┘    └──────────────────────────────────┘    │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PSI Layer (Static Analysis)                   │
│  • PsiMethod (selected by user)                                 │
│  • PsiClass (containing the method)                             │
│  • PSI tree represents code structure                           │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼  onMethodSelected() → executeMethod()
┌─────────────────────────────────────────────────────────────────┐
│                   Validation & Preparation                       │
│  ✓ Check if method is static                                   │
│  ✓ Check if method has zero parameters                         │
│  ✓ Get qualified class name                                    │
│  ✓ Find module for PSI element                                 │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼  getModuleClassLoader()
┌─────────────────────────────────────────────────────────────────┐
│                    ClassLoader Construction                      │
│  ┌──────────────────────────────────────────────────┐          │
│  │ OrderEnumerator.orderEntries(module)             │          │
│  │   .withoutSdk()    // Skip JDK                   │          │
│  │   .recursively()   // Include transitive deps    │          │
│  │   .classes()       // Get compiled classes       │          │
│  │   .getRoots()      // Get filesystem roots       │          │
│  └──────────────────────────────────────────────────┘          │
│                           │                                      │
│  Classpath Entries:       ▼                                     │
│  • /project/target/classes                                      │
│  • /maven-repo/j2html-1.6.0.jar                                │
│  • /maven-repo/other-deps.jar                                  │
│                           │                                      │
│                           ▼                                      │
│  URLClassLoader (custom classloader with full classpath)       │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼  Class.forName()
┌─────────────────────────────────────────────────────────────────┐
│                    Runtime Layer (Reflection)                    │
│  • Load Class<?> from compiled bytecode                         │
│  • Get Method via getDeclaredMethod()                           │
│  • setAccessible(true) for non-public methods                   │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼  method.invoke(null)
┌─────────────────────────────────────────────────────────────────┐
│                      Method Execution                            │
│  • Invoke static method with null (no instance needed)          │
│  • Catch and handle any exceptions                              │
│  • Get result object (j2html Tag/DomContent)                    │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼  renderJ2HtmlObject()
┌─────────────────────────────────────────────────────────────────┐
│                      HTML Rendering                              │
│  • Get .render() method via reflection                          │
│  • Invoke render() on j2html object                             │
│  • Get HTML string result                                       │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼  displayRenderedHtml()
┌─────────────────────────────────────────────────────────────────┐
│                      Display Result                              │
│  ┌───────────────────────────────────────────────────┐          │
│  │ ✓ Phase 4 Success!                               │          │
│  │ Method executed and HTML rendered.               │          │
│  ├───────────────────────────────────────────────────┤          │
│  │ Rendered output from: buildMyDiv()               │          │
│  ├───────────────────────────────────────────────────┤          │
│  │ <div>Hello World</div>                           │          │
│  └───────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## Error Handling Flow

```
executeMethod()
    │
    ├──▶ psiClass == null? ──────────────▶ showError("Could not find containing class")
    │
    ├──▶ qualifiedName == null? ─────────▶ showError("Could not determine qualified class name")
    │
    ├──▶ !isStatic? ─────────────────────▶ showError("Method must be static...")
    │
    ├──▶ hasParameters? ─────────────────▶ showError("Method has parameters...")
    │
    ├──▶ module == null? ────────────────▶ showError("Could not find module")
    │
    ├──▶ ClassNotFoundException? ────────▶ showError("Class not found. Make sure project is compiled")
    │
    ├──▶ NoSuchMethodException? ─────────▶ showError("Method not found in compiled class")
    │
    ├──▶ InvocationException? ───────────▶ showError("Error invoking method: [cause]")
    │
    ├──▶ result == null? ────────────────▶ showError("Method returned null")
    │
    ├──▶ render() throws? ───────────────▶ showError("render() did not return a String")
    │
    └──▶ Success! ───────────────────────▶ displayRenderedHtml()
```

## Component Interaction

```
┌──────────────┐
│   PreviewPanel   │
│   (Main UI)      │
└────┬────────────┘
     │
     │ has
     ▼
┌──────────────────┐
│  methodSelector   │ ◀── User selects method
│  (JComboBox)      │
└────┬─────────────┘
     │
     │ triggers
     ▼
┌────────────────────┐
│ onMethodSelected() │
└────┬───────────────┘
     │
     │ calls
     ▼
┌──────────────────┐       ┌───────────────────────┐
│ executeMethod()  ├──────▶│ getModuleClassLoader() │
└────┬─────────────┘       └───────────────────────┘
     │                              │
     │ uses                         │ returns
     ▼                              ▼
┌──────────────────┐       ┌──────────────────┐
│ Class.forName()  │◀──────┤ URLClassLoader   │
└────┬─────────────┘       └──────────────────┘
     │
     │ loads
     ▼
┌──────────────────┐
│ Method.invoke()  │
└────┬─────────────┘
     │
     │ returns j2html object
     ▼
┌──────────────────────┐
│ renderJ2HtmlObject() │
└────┬─────────────────┘
     │
     │ returns HTML string
     ▼
┌───────────────────────┐      ┌─────────────┐
│ displayRenderedHtml() ├─────▶│ htmlPreview │
└───────────────────────┘      │ (JEditorPane)│
                               └─────────────┘
```

## PSI to Runtime Bridge

```
Static Analysis (PSI)          Dynamic Execution (Runtime)
─────────────────────          ─────────────────────────────

PsiMethod                 ───▶  java.lang.reflect.Method
  .getName()                      .invoke()
  .getContainingClass()           .setAccessible()
  .hasModifierProperty()
  .getParameterList()

PsiClass                  ───▶  java.lang.Class<?>
  .getQualifiedName()             Class.forName()
  .getMethods()                   .getDeclaredMethod()

PsiFile                   ───▶  Compiled .class file
  Static structure                Runtime bytecode
  
Module                    ───▶  URLClassLoader
  PSI elements                    Classpath entries
                                  Loaded classes
```

## Data Flow

```
User Action
    ↓
Select "buildMyDiv()" from dropdown
    ↓
onMethodSelected()
    ↓
executeMethod(PsiMethod)
    ↓
Validate: static? ✓  params? 0 ✓
    ↓
getModuleClassLoader(module)
    ↓
Build classpath: [/target/classes, /j2html.jar, ...]
    ↓
Class.forName("com.example.Components")
    ↓
Method method = class.getDeclaredMethod("buildMyDiv")
    ↓
Object result = method.invoke(null)
    ↓
result = ContainerTag instance
    ↓
renderJ2HtmlObject(result)
    ↓
Method render = result.getClass().getMethod("render")
    ↓
String html = (String) render.invoke(result)
    ↓
html = "<div>Hello World</div>"
    ↓
displayRenderedHtml(html, psiMethod)
    ↓
Show green banner + formatted HTML in preview pane
    ↓
User sees rendered output! 🎉
```

## Key Decisions

### Why Custom ClassLoader?
- Plugin classloader doesn't have j2html
- Need to load both user classes AND dependencies
- IntelliJ projects have complex module structures
- Solution: Build URLClassLoader with OrderEnumerator

### Why Reflection?
- j2html not on plugin classpath (can't direct call)
- Need to bridge PSI (static) to runtime (dynamic)
- Want to execute arbitrary user methods
- Solution: Reflection for both invocation and rendering

### Why Static Methods Only (Phase 4a)?
- Simplifies implementation (no instance creation)
- Many j2html components are static factory methods
- Good starting point for learning
- Solution: Phase 5 will add non-static support

### Why Zero Parameters Only (Phase 4a)?
- No UI for parameter input yet
- Simplifies error handling
- Demonstrates core execution flow
- Solution: Phase 5 will add @J2HtmlPreview annotation

---

**Phase 4 is COMPLETE!** This diagram shows the full execution flow from user interaction to rendered HTML output. 🚀
