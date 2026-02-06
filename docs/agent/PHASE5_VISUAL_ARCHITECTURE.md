# Phase 5 Complete: Visual Architecture Diagram

## Expression Evaluation Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         USER INTERACTION                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                    User types in Expression Editor:
              userCard(new User("Alice", "alice@example.com"))
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    STEP 1: Generate Wrapper Class                        │
│                  (generateWrapperClass method)                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                         Wrapper Class Source:
                    ┌────────────────────────┐
                    │ package com.example;   │
                    │ import j2html.*;       │
                    │                        │
                    │ public class           │
                    │ ExpressionWrapper_123  │
                    │ {                      │
                    │   public static        │
                    │   Object eval() {      │
                    │     return userCard(   │
                    │       new User(...)    │
                    │     );                 │
                    │   }                    │
                    │ }                      │
                    └────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    STEP 2: Build Classpath                               │
│                    (buildClasspath method)                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                         Collect Dependencies:
        ┌──────────────────────────────────────────────────┐
        │  /project/target/classes                         │
        │  /m2/repository/j2html/j2html-1.6.0.jar         │
        │  /m2/repository/other-deps/...                   │
        └──────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 STEP 3: Compile Wrapper Class                            │
│                (compileAndLoadClass method - Part 1)                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                    Write to temporary file:
            /tmp/j2html_expr_xyz/ExpressionWrapper_123.java
                                    │
                                    ▼
                    JavaCompiler.compile():
              ┌──────────────────────────────────┐
              │ javac -classpath <classpath>     │
              │       -d /tmp/j2html_expr_xyz    │
              │       ExpressionWrapper_123.java │
              └──────────────────────────────────┘
                                    │
                        ┌───────────┴───────────┐
                        │                       │
                    Success                  Failure
                        │                       │
                        ▼                       ▼
                  Compiled                 Format Error
                   .class                  Messages with
                    file                   Line Numbers
                        │                       │
                        ▼                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                  STEP 4: Load Compiled Class                             │
│               (compileAndLoadClass method - Part 2)                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                        URLClassLoader:
              ┌────────────────────────────────┐
              │ Parent: Module ClassLoader     │
              │   ├─ Project Classes           │
              │   └─ Dependencies (j2html)     │
              │                                │
              │ Child: Expression ClassLoader  │
              │   └─ ExpressionWrapper_123     │
              └────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    STEP 5: Execute Expression                            │
│                   (evaluateAndDisplay method)                            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                    Reflection Call:
        Class.getDeclaredMethod("eval").invoke(null)
                                    │
                                    ▼
                  Expression Executes:
        userCard(new User("Alice", "alice@example.com"))
                                    │
                                    ▼
                      Returns j2html Object:
                         DivTag instance
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      STEP 6: Render to HTML                              │
│                    (renderJ2HtmlObject method)                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                   Call .render() method:
                 j2htmlObject.render()
                                    │
                                    ▼
                         HTML String:
            <div class="user-card">
              <h2>Alice</h2>
              <p>alice@example.com</p>
            </div>
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     STEP 7: Display in Preview                           │
│                  (displayRenderedHtml method)                            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                      JEditorPane shows:
                    ┌──────────────────┐
                    │                  │
                    │  Alice           │
                    │  ──────          │
                    │  alice@example   │
                    │  .com            │
                    │                  │
                    └──────────────────┘
```

## ClassLoader Hierarchy

```
┌────────────────────────────────────────────────────────────┐
│                    Bootstrap ClassLoader                    │
│                    (JDK System Classes)                     │
└────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│                   Plugin ClassLoader                        │
│              (IntelliJ Plugin Classes)                      │
└────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│                   Module ClassLoader                        │
│         (Created by getModuleClassLoader)                   │
│                                                             │
│  ┌────────────────────────────────────────────────┐        │
│  │ • Project Compiled Classes                     │        │
│  │   /project/target/classes/com/example/*.class  │        │
│  │                                                 │        │
│  │ • Maven Dependencies                            │        │
│  │   /m2/repository/j2html/j2html-1.6.0.jar      │        │
│  │   /m2/repository/...                           │        │
│  └────────────────────────────────────────────────┘        │
└────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│              Expression URLClassLoader                      │
│         (Created by compileAndLoadClass)                    │
│                                                             │
│  ┌────────────────────────────────────────────────┐        │
│  │ • Temporary Directory                           │        │
│  │   /tmp/j2html_expr_xyz/                        │        │
│  │                                                 │        │
│  │ • Compiled Wrapper Class                        │        │
│  │   ExpressionWrapper_123.class                   │        │
│  └────────────────────────────────────────────────┘        │
└────────────────────────────────────────────────────────────┘
```

## Component Interaction

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│                 │         │                  │         │                 │
│  Expression     │────────▶│  PreviewPanel    │────────▶│  JavaCompiler   │
│  Editor         │  Text   │                  │ Wrapper │  API            │
│  (TextField)    │         │  Phase 5 Core    │  Code   │                 │
│                 │         │                  │         │                 │
└─────────────────┘         └──────────────────┘         └─────────────────┘
                                     │                            │
                                     │                            │
                                     ▼                            ▼
                            ┌──────────────────┐       ┌─────────────────┐
                            │                  │       │                 │
                            │  generateWrapper │       │  Temp Files     │
                            │  Class()         │       │  /tmp/...       │
                            │                  │       │                 │
                            └──────────────────┘       └─────────────────┘
                                     │                            │
                                     │                            │
                                     ▼                            ▼
                            ┌──────────────────┐       ┌─────────────────┐
                            │                  │       │                 │
                            │  buildClasspath()│       │  .java file     │
                            │                  │       │  .class file    │
                            │                  │       │                 │
                            └──────────────────┘       └─────────────────┘
                                     │                            │
                                     │                            │
                                     ▼                            ▼
                            ┌──────────────────┐       ┌─────────────────┐
                            │                  │       │                 │
                            │  compileAndLoad  │◀──────│  Compiled       │
                            │  Class()         │       │  Class          │
                            │                  │       │                 │
                            └──────────────────┘       └─────────────────┘
                                     │
                                     │
                                     ▼
                            ┌──────────────────┐
                            │                  │
                            │  Reflection:     │
                            │  eval() invoke   │
                            │                  │
                            └──────────────────┘
                                     │
                                     │
                                     ▼
                            ┌──────────────────┐
                            │                  │
                            │  j2html Object   │
                            │  .render()       │
                            │                  │
                            └──────────────────┘
                                     │
                                     │
                                     ▼
                            ┌──────────────────┐
                            │                  │
                            │  HTML Preview    │
                            │  (JEditorPane)   │
                            │                  │
                            └──────────────────┘
```

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    User Input                                │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
            ┌───────────────────────┐
            │ Expression Validation │
            └───────────────────────┘
                        │
            ┌───────────┴───────────┐
            │                       │
         Valid                  Empty/Null
            │                       │
            ▼                       ▼
    ┌──────────────┐        ┌─────────────┐
    │ Generate     │        │ Show Error: │
    │ Wrapper      │        │ "No expr"   │
    └──────────────┘        └─────────────┘
            │
            ▼
    ┌──────────────┐
    │ Compile      │
    └──────────────┘
            │
    ┌───────┴───────┐
    │               │
Success          Failure
    │               │
    ▼               ▼
┌────────┐   ┌──────────────────┐
│ Load   │   │ Show Diagnostics:│
│ Class  │   │ Line 3: Missing )│
└────────┘   │ Line 5: Unknown  │
    │        └──────────────────┘
    ▼
┌────────┐
│ Invoke │
│ eval() │
└────────┘
    │
┌───┴────┐
│        │
OK    Exception
│        │
▼        ▼
Result   Show Error:
│        "Runtime error"
▼
Check Type
│
┌────┴────┐
│         │
j2html  Other
│         │
▼         ▼
Render    Show Error:
│         "Not j2html"
▼
Display
```

## Key Design Decisions

### 1. Wrapper Class Strategy
**Why**: Can't compile raw expressions
**How**: Generate complete Java class with imports
**Benefit**: Full Java language support

### 2. Temporary Files
**Why**: JavaCompiler works with files
**How**: Use Files.createTempDirectory()
**Cleanup**: deleteOnExit() + GC

### 3. ClassLoader Hierarchy
**Why**: Need both project classes and compiled expression
**How**: Parent = Module, Child = Expression
**Benefit**: Proper isolation and access

### 4. Reflection for Invocation
**Why**: Don't know return type at compile time
**How**: Method.invoke() with setAccessible(true)
**Benefit**: Works with any j2html type

### 5. Diagnostic Collection
**Why**: Provide useful error messages
**How**: DiagnosticCollector with JavaCompiler
**Benefit**: Line numbers and detailed errors

## Performance Profile

```
Operation                    Time          Notes
─────────────────────────────────────────────────────────────
Generate Wrapper            < 1ms         String operations
Build Classpath             1-5ms         File system queries
Write Temp File             1-3ms         Small files
Compile                     100-500ms     First time slower
Load Class                  1-5ms         URLClassLoader
Invoke eval()               1-10ms        Depends on expression
Render HTML                 1-5ms         j2html.render()
Display                     5-20ms        Swing UI update
─────────────────────────────────────────────────────────────
TOTAL                       200-800ms     Typical case
```

## Security Considerations

### ✅ Safe Aspects
- Expressions run in user's own project context
- Uses project's classpath (not plugin classpath)
- No arbitrary code from external sources
- Standard Java security model applies

### ⚠️ Considerations
- User can execute any Java code (by design)
- Temporary files in shared temp directory
- ClassLoaders kept alive until GC
- No timeout on compilation/execution

### Production Hardening (Future)
- Add compilation timeout
- Add execution timeout
- Set file permissions on temp files
- Implement ClassLoader cleanup
- Add memory limits
