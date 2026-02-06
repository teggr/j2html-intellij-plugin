# How to Use the New Debug Logging

## What Changed

### Fixed
1. **EDT Violation** - No more "Slow operations are prohibited on EDT" warnings
2. **Console Logging** - Debug output now goes to console (System.err) where it's always visible

### How to See Debug Output

The plugin now prints detailed debug information to **standard error (System.err)**, which appears in:
- **IntelliJ IDEA Console** - The Run/Debug console at the bottom of the IDE
- **Terminal/Command Line** - If running IntelliJ from terminal
- **idea.log** - IntelliJ's log file (though console is easier)

## What You'll See

When you click "Compile and Preview", the console will show:

### 1. Classpath Building
```
=== BUILD CLASSPATH DEBUG ===
Building classpath for module: your-module-name
Found 25 classpath roots from OrderEnumerator
Processing classpath entry: jar://C:/Users/robin/.m2/repository/com/j2html/j2html/1.6.0/j2html-1.6.0.jar!/
  After jar:// cleanup: C:/Users/robin/.m2/repository/com/j2html/j2html/1.6.0/j2html-1.6.0.jar
  Normalized to: C:\Users\robin\.m2\repository\com\j2html\j2html\1.6.0\j2html-1.6.0.jar
  File exists: true
Processing classpath entry: [next entry]
...
Built classpath with 25 entries for compilation
Full classpath: C:\project\target\classes;C:\Users\robin\.m2\...\j2html-1.6.0.jar;...
=== END BUILD CLASSPATH DEBUG ===
```

### 2. Javac Command
```
=== JAVAC COMMAND DEBUG ===
Executing javac command:
  javac: C:\Program Files\Eclipse Adoptium\jdk-24.0.2.12-hotspot\bin\javac.exe
  -classpath: C:\project\target\classes;C:\Users\robin\.m2\...\j2html-1.6.0.jar;...
  -d: C:\Users\robin\AppData\Local\Temp\j2html_expr_123456789\
  source: C:\Users\robin\AppData\Local\Temp\j2html_expr_123456789\ExpressionWrapper_123.java
Full command: C:\...\javac.exe -classpath ... -d ... ...
=== END JAVAC COMMAND DEBUG ===
```

## What to Check

### ✅ Success Indicators

1. **J2HTML in Classpath**
   - Look for line: `Processing classpath entry: jar://...j2html...jar`
   - Should show `File exists: true`
   - Should appear in "Full classpath:" line

2. **Proper Path Format**
   - Windows: `C:\Users\...` (backslashes)
   - No `jar://` prefix in normalized path
   - No `!/` suffix

3. **Multiple Entries**
   - Should see 10-30+ entries typically
   - Project output directory (e.g., `target/classes`)
   - Dependencies (JARs in .m2/repository)

### ❌ Problem Indicators

1. **No J2HTML Entry**
   - If j2html never appears in the processing list
   - **Solution:** Check project dependencies, run Maven/Gradle sync

2. **File Doesn't Exist**
   - If any entry shows `File exists: false`
   - **Solution:** Path normalization issue or missing file

3. **Very Few Entries**
   - If "Found 0 classpath roots" or only 1-2
   - **Solution:** Module not loaded, dependencies not resolved

4. **Wrong Path Format**
   - Still seeing `/C:/` instead of `C:\`
   - Still seeing `jar://` prefix
   - **Solution:** Path normalization logic needs fix

## Next Steps Based on Output

### Scenario A: J2HTML in Classpath But Still Fails

**What it means:** Classpath is correct, but javac can't use it properly.

**Possible causes:**
- Classpath too long (Windows command line limit ~8191 chars)
- Special characters in paths not handled
- ProcessBuilder issue

**Solutions:**
1. Use javac response file (@argfile)
2. Shorten paths (symbolic links)
3. Write classpath to temp file

### Scenario B: J2HTML Not in Classpath

**What it means:** OrderEnumerator not finding j2html dependency.

**Possible causes:**
- j2html not in project dependencies
- Dependency scope wrong (test-only?)
- IntelliJ hasn't synced project

**Solutions:**
1. Check pom.xml/build.gradle for j2html dependency
2. Run Maven/Gradle sync
3. Try `OrderEnumerator.orderEntries(module).withSdk()` instead
4. Use different API (JavaParameters)

### Scenario C: Wrong Path Format

**What it means:** Path normalization not working correctly.

**Solutions:**
1. Debug specific path format from VirtualFile
2. Additional path cleaning logic
3. Use VirtualFile.getCanonicalPath() or other methods

## How to Share Debug Output

When reporting issues, please:

1. **Copy full console output** from === BUILD CLASSPATH DEBUG === to === END JAVAC COMMAND DEBUG ===
2. **Include compilation error** if any
3. **Mention:** Operating system and Java version

## Expected Behavior

### Working System

```
=== BUILD CLASSPATH DEBUG ===
Building classpath for module: demo
Found 23 classpath roots from OrderEnumerator
Processing classpath entry: C:/project/target/classes
  Normalized to: C:\project\target\classes
  File exists: true
Processing classpath entry: jar://C:/Users/robin/.m2/.../j2html-1.6.0.jar!/
  After jar:// cleanup: C:/Users/robin/.m2/.../j2html-1.6.0.jar
  Normalized to: C:\Users\robin\.m2\...\j2html-1.6.0.jar
  File exists: true
[... more entries ...]
Full classpath: C:\project\target\classes;C:\...\j2html-1.6.0.jar;...
=== END BUILD CLASSPATH DEBUG ===

=== JAVAC COMMAND DEBUG ===
[... shows j2html.jar in classpath ...]
=== END JAVAC COMMAND DEBUG ===

[Compilation succeeds, HTML renders]
```

### Failing System (No J2HTML)

```
=== BUILD CLASSPATH DEBUG ===
Building classpath for module: demo
Found 5 classpath roots from OrderEnumerator
Processing classpath entry: C:/project/target/classes
  Normalized to: C:\project\target\classes
  File exists: true
[... only project classes, no j2html ...]
Full classpath: C:\project\target\classes;...
=== END BUILD CLASSPATH DEBUG ===

Error: package j2html does not exist
```

## Troubleshooting Tips

### Can't See Console Output

1. **Check Console Tab** - Bottom of IntelliJ window
2. **Enable if Hidden** - View → Tool Windows → Run/Debug
3. **Check idea.log** - Help → Show Log in Explorer
4. **Run from Terminal** - Console output appears there

### Too Much Output

The debug logging is intentionally verbose to diagnose the issue. Once we identify the problem, we'll reduce it to normal levels.

### Still Can't Find Issue

If the console output shows j2html in the classpath with correct paths, but compilation still fails, we may need to:
1. Test javac command manually
2. Check file permissions
3. Try alternative compilation approaches

## Summary

With this debug logging, we'll be able to see exactly:
- ✅ What dependencies IntelliJ finds
- ✅ How paths are transformed
- ✅ What goes to javac
- ✅ Why compilation fails

This should definitively identify the classpath issue!
