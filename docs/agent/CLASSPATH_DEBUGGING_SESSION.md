# Classpath Debugging Session Summary

## Issue History

### Initial Problem
Process-based javac compilation (fallback for Java 9+ with IntelliJ on JRE) was successfully invoking javac, but javac couldn't find j2html packages:

```
Error: package j2html does not exist
import j2html.TagCreator;
```

### Investigation Steps

## Step 1: Path Normalization (Commits 61f3d82, 5b18ffa, 24fe1ca)

**Hypothesis:** IntelliJ VirtualFile paths (`/C:/path`) incompatible with external javac on Windows.

**Changes:**
- Added Windows path handling (regex to detect `/C:/` format)
- Used `File.getAbsolutePath()` to normalize paths
- Improved logging levels (INFO for count, DEBUG for full classpath)

**Result:** Path normalization implemented correctly, but issue persisted.

## Step 2: Enhanced Debugging (Commit 787e82a)

**Hypothesis:** Need to see exactly what's in the classpath to diagnose.

**Changes:**
Added verbose INFO-level logging to `buildClasspath()`:
- Log module name
- Log count of roots from OrderEnumerator
- Log each entry: raw path → after jar:// cleanup → normalized → exists check
- Log full classpath string
- Log complete javac command

**Expected Result:** Detailed logs will reveal:
1. If OrderEnumerator finds j2html
2. If path processing works correctly  
3. If files exist on filesystem
4. What final classpath looks like

## Step 3: Include SDK Dependencies (Commit d135feb)

**Hypothesis:** `.withoutSdk()` might be excluding necessary dependencies.

**Changes:**
```java
// Before
VirtualFile[] roots = OrderEnumerator.orderEntries(module)
    .withoutSdk()
    .recursively()
    .classes()
    .getRoots();

// After  
VirtualFile[] roots = OrderEnumerator.orderEntries(module)
    .recursively()
    .classes()
    .getRoots();
```

**Rationale:**
- More dependencies in classpath is safer for external process
- JDK classes in classpath won't cause issues
- Different than getModuleClassLoader which works with API

## Current Status

### What We've Done

1. ✅ Fixed path normalization for Windows
2. ✅ Added comprehensive logging
3. ✅ Created debugging guide (DEBUGGING_CLASSPATH_COLLECTION.md)
4. ✅ Removed SDK exclusion from classpath
5. ✅ Documented all changes

### What We Need

**User to test and provide:**
1. Run expression compilation
2. Share IntelliJ log output showing:
   - "Building classpath for module: ..."
   - "Found N classpath roots..."
   - All "Processing classpath entry: ..." lines
   - "Full classpath: ..." line
   - "Executing javac command: ..." line
   - Compilation error (if still fails)

### Expected Log Output

```
INFO - Building classpath for module: my-project
INFO - Found 25 classpath roots from OrderEnumerator
INFO - Processing classpath entry: jar://C:/Users/robin/.m2/repository/com/j2html/j2html/1.6.0/j2html-1.6.0.jar!/
INFO -   After jar:// cleanup: C:/Users/robin/.m2/repository/com/j2html/j2html/1.6.0/j2html-1.6.0.jar
INFO -   Normalized to: C:\Users\robin\.m2\repository\com\j2html\j2html\1.6.0\j2html-1.6.0.jar
INFO -   File exists: true
[... more entries ...]
INFO - Built classpath with 25 entries for compilation
INFO - Full classpath: C:\project\target\classes;C:\Users\robin\.m2\...\j2html-1.6.0.jar;...
INFO - Executing javac command: C:\...\javac.exe -classpath C:\...\j2html.jar;... -d C:\temp\... C:\temp\...\Source.java
```

## Possible Remaining Issues

### If J2HTML Not in Classpath

**Diagnosis:** Logs show no j2html entry

**Possible Causes:**
1. J2HTML not in project dependencies
2. Dependency scope issue (test-only?)
3. IntelliJ hasn't synced dependencies
4. Module not the correct one

**Solutions:**
- Verify j2html in pom.xml/build.gradle
- Check dependency scope is compile/runtime
- Trigger Maven/Gradle sync
- Use different module or check module selection

### If Classpath Empty or Very Few Entries

**Diagnosis:** "Found 0 classpath roots" or very few

**Possible Causes:**
1. Module not properly loaded
2. Dependencies not resolved
3. OrderEnumerator misconfigured

**Solutions:**
- Check project is synced and built
- Verify module is correct
- Try different OrderEnumerator options
- Use alternative API (JavaParameters, CompilerConfiguration)

### If Paths Malformed

**Diagnosis:** Normalized paths don't exist or have wrong format

**Possible Causes:**
1. Edge case in path cleaning
2. File.getAbsolutePath() behaving unexpectedly
3. Unusual path format from IntelliJ

**Solutions:**
- Debug specific path format
- Add more path cleaning logic
- Use alternative path conversion

### If Everything Looks Correct But Still Fails

**Diagnosis:** Logs show j2html in correct format, but javac still can't find it

**Possible Causes:**
1. ProcessBuilder classpath handling issue
2. Long classpath length limits
3. Special characters in paths
4. File access permissions

**Solutions:**
- Use javac response file (@argfile)
- Write classpath to temp file
- Test paths are accessible from separate process
- Try shorter paths or path substitution

## Alternative Approaches if Current Method Fails

### Option 1: Use CompilerManager for Everything

Instead of manual javac invocation, always use IntelliJ's CompilerManager.

**Pros:**
- IntelliJ handles all classpath logic
- No need to deal with process invocation
- Consistent with rest of IntelliJ

**Cons:**
- More complex (async, requires callback)
- Slower (full compilation)
- Might not work if compilation fails for other reasons

### Option 2: Use JavaCompiler with Custom ClassLoader

Create a custom classloader that loads the compiler from the project JDK.

**Pros:**
- In-process (faster)
- Direct API access

**Cons:**
- Complex classloader manipulation
- Module system issues with Java 9+
- Might not work reliably

### Option 3: Use JavaParameters API

Use IntelliJ's JavaParameters to build classpath.

```java
JavaParameters params = new JavaParameters();
params.configureByModule(module, JavaParameters.JDK_AND_CLASSES_AND_TESTS);
String classpath = params.getClassPath().getPathsString();
```

**Pros:**
- Uses IntelliJ's built-in classpath logic
- Should match what IDE uses

**Cons:**
- Might still have same issues
- Need to import additional classes

### Option 4: Parse Compiler Output Path

Use CompilerProjectExtension or CompilerConfiguration to get output paths explicitly.

```java
CompilerProjectExtension extension = CompilerProjectExtension.getInstance(project);
VirtualFile outputDir = extension.getCompilerOutput();
```

**Pros:**
- Explicit control
- Can add specific paths

**Cons:**
- Need to manually find all dependencies
- Complex logic

## Recommendations

### Immediate

1. **Get logs from user** - This is critical to diagnose
2. **Check if j2html in classpath** - Should be visible in logs
3. **Verify files exist** - "File exists: true" for j2html

### If J2HTML Missing

1. Try JavaParameters API approach
2. Check project configuration
3. Manually add j2html path if needed

### If Still Failing

1. Implement response file approach
2. Consider CompilerManager fallback
3. Add explicit j2html path finding logic

## Success Criteria

✅ **Logs show j2html in classpath**
✅ **All normalized paths exist on filesystem**
✅ **Javac command includes correct classpath**
✅ **Compilation succeeds**
✅ **Expression evaluates and renders HTML**

## Documentation

- **PROCESS_BASED_JAVAC_IMPLEMENTATION.md** - Process-based compilation architecture
- **CLASSPATH_PATH_NORMALIZATION_FIX.md** - Path normalization details
- **DEBUGGING_CLASSPATH_COLLECTION.md** - Current debugging guide
- **This file** - Complete debugging session history

## Next Actions

1. **User tests latest version**
2. **User shares complete logs**
3. **Analyze logs per debugging guide**
4. **Apply targeted fix based on findings**
5. **Reduce logging verbosity for production**
6. **Mark as complete when working**
