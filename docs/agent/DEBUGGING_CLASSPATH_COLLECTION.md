# Debugging Classpath Collection for Process-Based Javac

## Expected Log Output

When expression compilation is attempted, you should see logs like:

```
INFO - Building classpath for module: [module-name]
INFO - Found N classpath roots from OrderEnumerator
INFO - Processing classpath entry: jar://C:/Users/robin/.m2/repository/com/j2html/j2html/1.6.0/j2html-1.6.0.jar!/
INFO -   After jar:// cleanup: C:/Users/robin/.m2/repository/com/j2html/j2html/1.6.0/j2html-1.6.0.jar
INFO -   Normalized to: C:\Users\robin\.m2\repository\com\j2html\j2html\1.6.0\j2html-1.6.0.jar
INFO -   File exists: true
INFO - Processing classpath entry: [next entry]
...
INFO - Built classpath with N entries for compilation
INFO - Full classpath: C:\path1;C:\path2;...
INFO - Executing javac command: C:\path\to\javac.exe -classpath C:\path1;C:\path2;... -d C:\temp\... C:\temp\...\Source.java
```

## What to Check

### 1. Number of Classpath Roots

**Look for:** `Found N classpath roots from OrderEnumerator`

**Expected:** At least several entries (typically 5-20 depending on project dependencies)

**If 0 or very few:** OrderEnumerator is not finding dependencies. This could mean:
- Module is not properly loaded
- Dependencies not resolved in IntelliJ
- Need to trigger project sync/build first

### 2. J2HTML JAR Presence

**Look for:** An entry containing `j2html` in the path, like:
```
Processing classpath entry: jar://C:/Users/robin/.m2/repository/com/j2html/j2html/1.6.0/j2html-1.6.0.jar!/
```

**Expected:** Should find at least one j2html JAR file

**If missing:** j2html dependency not being included by OrderEnumerator. Possible solutions:
- Check project's pom.xml or build.gradle has j2html dependency
- Try including SDK: change `.withoutSdk()` to `.withSdk()` or add separately
- Check if need `.productionOnly()` or `.exportedOnly()` instead

### 3. Path Normalization

**Look for:** 
```
After jar:// cleanup: C:/Users/robin/.m2/.../j2html-1.6.0.jar
Normalized to: C:\Users\robin\.m2\...\j2html-1.6.0.jar
```

**Expected:** 
- Unix-style paths (`/C:/...`) converted to Windows paths (`C:\...`)
- Forward slashes converted to backslashes on Windows
- No `jar://` protocol prefix
- No `!/` JAR entry suffix

**If incorrect:** Path normalization logic needs adjustment

### 4. File Existence

**Look for:** `File exists: true` for each entry

**Expected:** All classpath entries should exist on filesystem

**If false:** Path normalization is producing incorrect paths. Check:
- Is the path format correct for the OS?
- Are there any extra/missing characters?
- Is the file actually at that location?

### 5. Final Classpath String

**Look for:** `Full classpath: C:\path1;C:\path2;...`

**Expected:**
- Multiple paths separated by `;` (Windows) or `:` (Unix)
- Should include j2html JAR path
- All paths should be in native OS format

**If incorrect:** Check path separator and path format

### 6. Javac Command

**Look for:** `Executing javac command: ...`

**Expected:**
```
C:\path\to\javac.exe -classpath C:\path1;C:\path2;... -d C:\temp\... C:\temp\...\Source.java
```

**Check:**
- javac.exe path is correct
- -classpath argument includes j2html
- -d output directory exists
- Source file path is correct

## Common Issues and Solutions

### Issue 1: No Classpath Entries Found

**Symptom:** `Found 0 classpath roots from OrderEnumerator`

**Solutions:**
1. Check if project is properly loaded and synced
2. Try different OrderEnumerator options:
   ```java
   OrderEnumerator.orderEntries(module)
       .recursively()
       .classes()
       .getRoots()
   ```
   (Try with and without `.withoutSdk()`)

3. Check if module is the correct one

### Issue 2: J2HTML Not in Classpath

**Symptom:** No j2html entry in the logs

**Solutions:**
1. Verify j2html dependency in project's build file
2. Check dependency scope (compile/runtime)
3. Try including SDK libraries
4. Check if `.recursively()` is needed

### Issue 3: Path Format Issues

**Symptom:** Paths not being normalized correctly

**Solutions:**
1. Debug the exact format coming from `VirtualFile.getPath()`
2. Verify `File.getAbsolutePath()` behavior on the specific OS
3. Check for edge cases in path cleaning logic

### Issue 4: Javac Can't Read Classpath

**Symptom:** Compilation fails even with correct classpath in logs

**Solutions:**
1. Check for spaces in paths - might need quoting
2. Verify path separator is correct for OS
3. Test if paths are accessible from javac process
4. Check file permissions

## Next Steps Based on Logs

### If Classpath is Empty
- Fix OrderEnumerator configuration
- Ensure module dependencies are resolved

### If J2HTML Missing
- Add logic to explicitly include j2html
- Check dependency resolution
- Verify build file configuration

### If Paths Malformed
- Fix path normalization logic
- Handle edge cases (spaces, special chars)
- Test on different OS

### If Everything Looks Correct But Still Fails
- Issue might be with how ProcessBuilder handles the classpath
- May need to write classpath to a file and use `-cp @file` syntax
- May need to use response file (`@argfile`) for long classpaths
- Check if javac process has access to the files

## Testing the Fix

Once the classpath is correct in the logs, test by:
1. Running expression compilation
2. Verifying j2html classes are found
3. Confirming HTML renders correctly
4. Testing with different expressions

If compilation succeeds, reduce logging verbosity for production.
