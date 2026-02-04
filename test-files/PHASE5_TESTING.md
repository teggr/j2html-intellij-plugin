# Phase 5 Testing Guide

This directory contains test files for validating Phase 5 Complete: Full Expression Evaluation with JavaCompiler.

## Test Files

### ExampleJ2HtmlComponents.java
- Phase 3 & 4 test methods
- Methods with and without parameters
- Various return types (ContainerTag, DivTag, DomContent)

### Phase5ExampleWithObjects.java
- Phase 5 specific test cases
- Methods with custom object parameters (User class)
- Demonstrates object construction in expressions
- Multi-parameter methods
- Static method calls within expressions

## How to Test Phase 5

### Setup
1. Run the plugin: `./gradlew runIde`
2. Open a project that includes j2html dependency
3. Copy one of the test files into your project's `src/main/java/com/example/demo/` directory
4. Make sure the project is compiled
5. Open the test file in IntelliJ
6. Open the j2html Preview tool window (View → Tool Windows → j2html Preview)

### Test Cases

#### Test 1: Simple Parameter Expression
**File:** ExampleJ2HtmlComponents.java
**Method:** `userCard(String name, String email)`
**Steps:**
1. Select `userCard` from the method dropdown
2. Expression editor appears with template: `userCard("", "")`
3. Edit to: `userCard("Alice", "alice@example.com")`
4. Click "Compile and Preview"
5. **Expected:** HTML renders showing:
   ```html
   <div class="user-card">
     <h2>Alice</h2>
     <p>alice@example.com</p>
   </div>
   ```

#### Test 2: Object Construction
**File:** Phase5ExampleWithObjects.java
**Method:** `userCard(User user)`
**Steps:**
1. Select `userCard(User user)` from dropdown
2. Expression editor shows: `userCard(new User())`
3. Edit to: `userCard(new User("Bob", "bob@example.com"))`
4. Click "Compile and Preview"
5. **Expected:** HTML renders showing Bob's card with "User profile"

#### Test 3: Multiple Parameters with Object
**File:** Phase5ExampleWithObjects.java
**Method:** `userCard(User user, String theme)`
**Steps:**
1. Select the overloaded `userCard(User user, String theme)`
2. Expression editor shows: `userCard(new User(), "")`
3. Edit to: `userCard(new User("Carol", "carol@example.com"), "dark")`
4. Click "Compile and Preview"
5. **Expected:** HTML renders showing Carol's card with "Theme: dark"

#### Test 4: Static Method Call
**File:** Phase5ExampleWithObjects.java
**Method:** `userCard(User user)`
**Steps:**
1. Select `userCard(User user)`
2. Edit expression to: `userCard(createUser("Dave"))`
3. Click "Compile and Preview"
4. **Expected:** HTML renders showing Dave with email "dave@example.com"

#### Test 5: Multi-Line Expression
**File:** Phase5ExampleWithObjects.java
**Method:** `richUserCard(User user, String theme, int followers)`
**Steps:**
1. Select `richUserCard`
2. Edit expression to:
   ```java
   richUserCard(
       new User(
           "Eve",
           "eve@example.com"
       ),
       "light",
       1234
   )
   ```
3. Click "Compile and Preview"
4. **Expected:** HTML renders showing Eve's rich card with 1234 followers

#### Test 6: Complex Parameters
**File:** Phase5ExampleWithObjects.java
**Method:** `productDisplay(String name, double price, boolean inStock)`
**Steps:**
1. Select `productDisplay`
2. Edit expression to: `productDisplay("Laptop", 999.99, true)`
3. Click "Compile and Preview"
4. **Expected:** HTML renders showing product with price and "In Stock"

#### Test 7: Compilation Error Handling
**File:** Any
**Steps:**
1. Select any method with parameters
2. Enter invalid Java: `userCard(new User("Alice", )` (missing second parameter)
3. Click "Compile and Preview"
4. **Expected:** Error message showing compilation diagnostics with line number

#### Test 8: Using Static Imports
**File:** ExampleJ2HtmlComponents.java
**Method:** Any that returns a j2html type
**Steps:**
1. Note the file has: `import static j2html.TagCreator.*;`
2. For a parameterless method, you can also use the expression editor
3. Type: `div(h1("Direct usage"), p("No TagCreator prefix"))`
4. **Expected:** Works because static imports are included in wrapper

## Expected Behaviors

### Success Indicators
- ✅ Expression compiles without errors
- ✅ HTML renders in the preview pane
- ✅ Output matches expected structure
- ✅ Compilation time is reasonable (< 2 seconds for most expressions)

### Error Indicators
- ⚠️ Compilation errors shown with line numbers
- ⚠️ Clear error messages for missing parameters
- ⚠️ "No Java compiler available" if running on JRE instead of JDK

## Troubleshooting

### "No Java compiler available"
**Cause:** Running on JRE instead of JDK
**Fix:** Make sure IDEA is running on a JDK (not JRE)
**Check:** `java -version` should show JDK

### "Class not found" errors
**Cause:** Project not compiled
**Fix:** Build the project first (Build → Build Project)

### "Import not found" errors
**Cause:** Missing j2html dependency
**Fix:** Add j2html to project dependencies

### Compilation succeeds but ClassCastException
**Cause:** Expression returns wrong type (not a j2html object)
**Fix:** Ensure expression returns ContainerTag, DomContent, or similar j2html type

## Performance Notes

- First compilation after plugin load may take 1-2 seconds
- Subsequent compilations typically 200-800ms
- Complex expressions with many dependencies may take longer
- Temporary files are cleaned up automatically on JVM exit

## Next Steps

After validating Phase 5:
- Phase 5b: @J2HtmlPreview annotation support
- Phase 5c: Generate preview methods from expressions
- Phase 6: Live updates and safety features
