# Testing Phase 4: Compile-on-Demand Execution

This document describes how to test the Phase 4 compile-on-demand functionality.

## Prerequisites

1. JDK 17 or later installed
2. IntelliJ IDEA (for running the plugin)
3. The plugin project built and ready to run

## Running the Plugin

```bash
./gradlew runIde
```

This launches a sandbox IntelliJ instance with the plugin installed.

## Test Scenarios

### Test 1: Basic Execution (Main Scenario)

**Objective:** Verify that selecting a method triggers compilation and executes successfully.

**Steps:**
1. In the sandbox IntelliJ, create a new Java project
2. Add j2html dependency or use the test project structure
3. Create a class like `J2HtmlComponents.java` with this code:
   ```java
   package org.example;
   
   public class J2HtmlComponents {
       public static String buildMyDiv() {
           return "<div>Hello world!</div>";
       }
       
       public static String buildMyParagraph() {
           return "<p>This is a paragraph</p>";
       }
   }
   ```
4. Open the file in the editor
5. Open the "j2html Preview" tool window (View → Tool Windows → j2html Preview)
6. Select `buildMyDiv()` from the dropdown

**Expected Result:**
- Brief "Compiling..." message appears
- Green success banner: "✓ Rendered successfully"
- HTML output: `<div>Hello world!</div>`

**Success Criteria:** ✅ Compilation triggered automatically, HTML rendered

---

### Test 2: Compile After Code Change

**Objective:** Verify automatic recompilation after modifying code.

**Steps:**
1. With `J2HtmlComponents.java` open, modify `buildMyDiv()`:
   ```java
   public static String buildMyDiv() {
       return "<div>Updated hello world!</div>";
   }
   ```
2. **DO NOT** manually compile (don't press Ctrl+F9)
3. Select `buildMyDiv()` from the dropdown again

**Expected Result:**
- "Compiling..." message appears
- Compilation completes successfully
- Preview shows: `<div>Updated hello world!</div>`

**Success Criteria:** ✅ Plugin automatically recompiled changed code

---

### Test 3: Code With Compilation Errors

**Objective:** Verify error handling for compilation failures.

**Steps:**
1. Introduce a syntax error in `J2HtmlComponents.java`:
   ```java
   public static String buildMyDiv() {
       return "<div>Hello"  // Missing closing >"; and semicolon
   }
   ```
2. Select `buildMyDiv()` from dropdown

**Expected Result:**
- "Compiling..." message appears
- Red error banner: "Compilation failed with 1 error(s). Check the Problems panel for details."

**Steps to verify recovery:**
3. Fix the syntax error
4. Select the method again

**Expected Result:**
- Compilation succeeds
- HTML renders correctly

**Success Criteria:** ✅ Errors reported clearly, recovery works after fixing

---

### Test 4: Switch Between Methods

**Objective:** Verify switching between different methods works correctly.

**Steps:**
1. Ensure `J2HtmlComponents.java` has multiple methods:
   ```java
   public static String buildMyDiv() {
       return "<div>Hello world</div>";
   }
   
   public static String buildMyParagraph() {
       return "<p>A paragraph</p>";
   }
   
   public static String buildAForm() {
       return "<form><input type='text'></form>";
   }
   ```
2. Select `buildMyDiv()` → observe output
3. Select `buildMyParagraph()` → observe output
4. Select `buildAForm()` → observe output

**Expected Result:**
- Each selection triggers compilation (fast if nothing changed)
- Different HTML output displayed for each method
- No errors or UI glitches

**Success Criteria:** ✅ Method switching works smoothly

---

### Test 5: Compilation Aborted

**Objective:** Verify handling of aborted compilation.

**Steps:**
1. Create a large project or add artificial delay in build
2. Select a method to trigger compilation
3. Quickly stop the build process (Build → Stop Build)

**Expected Result:**
- Error message: "Compilation was aborted."

**Success Criteria:** ✅ Aborted compilation handled gracefully

---

## Technical Verification

### Thread Safety

**What to check:**
- No UI freezing during compilation
- No "IllegalStateException" or "Not on EDT" exceptions
- Smooth transitions between states

**How to verify:**
- Watch for console errors during testing
- Verify UI remains responsive while "Compiling..." is shown

### Asynchronous Behavior

**What to check:**
- "Compiling..." message appears immediately
- Plugin doesn't block the UI thread
- Compilation happens in background

**How to verify:**
- You should be able to click around IDE while compilation runs
- No "Application not responding" dialogs

---

## Success Criteria Summary

✅ Selecting a method triggers automatic compilation  
✅ "Compiling..." status shown while compilation runs  
✅ Successful compilation followed by method execution and HTML rendering  
✅ Compilation errors shown clearly with error count  
✅ Aborted compilation handled gracefully  
✅ UI remains responsive during compilation (async)  
✅ Works after code changes without manual Ctrl+F9  
✅ No thread-related crashes or warnings  

---

## Known Limitations

1. **Module Detection:** The plugin compiles at the module level. If the module can't be detected, an error is shown.
2. **Classpath:** Method execution uses a simple classloader. Complex projects with many dependencies may need additional classpath configuration.
3. **Incremental Compilation:** IntelliJ's compiler handles incremental builds. If nothing changed, compilation completes almost instantly.

---

## Troubleshooting

### "Could not find module for class"
- Ensure the file is part of a valid IntelliJ module
- Check that the project structure is properly configured

### "Class not found" after successful compilation
- Check the module output directory configuration
- Verify the class was actually compiled (check build directory)

### Compilation takes too long
- This is normal for large projects on first compile
- Subsequent compiles should be much faster (incremental)

### No methods appear in dropdown
- Ensure methods are:
  - `public`
  - `static`
  - Have zero parameters
  - Return a type name ending in "Tag" or "DomContent"
