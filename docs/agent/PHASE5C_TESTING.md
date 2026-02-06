# Phase 5c Manual Testing Guide

## Prerequisites
1. Build and install the plugin:
   ```bash
   ./gradlew :plugin:buildPlugin
   ```
2. Install the plugin from `plugin/build/distributions/plugin-0.1.0-SNAPSHOT.zip`
3. Restart IntelliJ IDEA
4. Open the j2html Preview tool window

## Test Scenario 1: Basic Expression to @Preview

### Setup
1. Open `test-files/Phase5ExampleWithObjects.java`
2. In the j2html Preview tool window, select `userCard(User user) → DivTag`
3. Expression evaluator panel should appear

### Test Steps
1. Type in expression editor: `userCard(new User("Alice", "alice@example.com"))`
2. Click Execute (▶) button
3. Verify the preview displays correctly
4. Click Save as @Preview (💾) button
5. In the dialog, enter: `Alice's Card`
6. Click OK

### Expected Result
A new method should be inserted at the end of the class:
```java
/**
 * Preview: Alice's Card
 */
@Preview(name = "Alice's Card")
public static DivTag userCard_alice_s_card() {
    return userCard(new User("Alice", "alice@example.com"));
}
```

### Verification
1. The method appears in the source file
2. The method selector dropdown now shows "Alice's Card" as an option
3. Selecting "Alice's Card" from the dropdown displays the same preview
4. The method name is valid Java (no syntax errors)

## Test Scenario 2: Multiple Parameters

### Setup
1. Use the same file: `test-files/Phase5ExampleWithObjects.java`
2. Select `userCard(User user, String theme) → DivTag`

### Test Steps
1. Type expression: `userCard(new User("Bob", "bob@example.com"), "dark")`
2. Click Execute (▶)
3. Verify preview
4. Click Save as @Preview (💾)
5. Enter name: `Bob's Dark Card`
6. Click OK

### Expected Result
```java
/**
 * Preview: Bob's Dark Card
 */
@Preview(name = "Bob's Dark Card")
public static DivTag userCard_bob_s_dark_card() {
    return userCard(new User("Bob", "bob@example.com"), "dark");
}
```

## Test Scenario 3: Complex Expression with Method Call

### Setup
1. Same file, select `userCard(User user) → DivTag`

### Test Steps
1. Type expression: `userCard(createUser("Charlie"))`
2. Click Execute (▶)
3. Verify preview
4. Click Save as @Preview (💾)
5. Enter name: `Charlie via Factory`
6. Click OK

### Expected Result
```java
/**
 * Preview: Charlie via Factory
 */
@Preview(name = "Charlie via Factory")
public static DivTag userCard_charlie_via_factory() {
    return userCard(createUser("Charlie"));
}
```

## Test Scenario 4: Special Characters in Name

### Setup
1. Same file, select `productDisplay(String name, double price, boolean inStock) → ContainerTag`

### Test Steps
1. Type expression: `productDisplay("Laptop", 999.99, true)`
2. Click Execute (▶)
3. Verify preview
4. Click Save as @Preview (💾)
5. Enter name: `Product: Laptop @ $999.99 (In Stock)`
6. Click OK

### Expected Result
- Method name should have special characters converted to underscores
- Preview name in annotation should preserve original text
```java
/**
 * Preview: Product: Laptop @ $999.99 (In Stock)
 */
@Preview(name = "Product: Laptop @ $999.99 (In Stock)")
public static ContainerTag productDisplay_product_laptop_999_99_in_stock() {
    return productDisplay("Laptop", 999.99, true);
}
```

## Test Scenario 5: Duplicate Names

### Setup
1. Same file, select `userCard(User user) → DivTag`

### Test Steps
1. Type expression: `userCard(new User("Test", "test@test.com"))`
2. Click Save as @Preview (💾)
3. Enter name: `Test`
4. Repeat steps 1-3 two more times with the same name

### Expected Result
Three methods should be created with unique names:
```java
@Preview(name = "Test")
public static DivTag userCard_test() { ... }

@Preview(name = "Test")
public static DivTag userCard_test1() { ... }

@Preview(name = "Test")
public static DivTag userCard_test2() { ... }
```

## Test Scenario 6: Edge Cases

### Empty Expression
1. Select a method
2. Leave expression editor empty
3. Click Save as @Preview (💾)
4. **Expected**: Error message "Expression is empty"

### No Method Selected
1. Don't select any method
2. Click Save as @Preview (💾)
3. **Expected**: Error message "No method selected"

### Cancel Dialog
1. Select a method and enter expression
2. Click Save as @Preview (💾)
3. Click Cancel in dialog
4. **Expected**: No action, no error

### Empty Name
1. Select a method and enter expression
2. Click Save as @Preview (💾)
3. Enter empty name or just spaces
4. Click OK
5. **Expected**: No action, no error

## Test Scenario 7: Long Method Names

### Test Steps
1. Enter expression: `userCard(new User("Test", "test@test.com"))`
2. Click Save as @Preview (💾)
3. Enter name: `This is a very long preview name with many words to test the method name generation`
4. Click OK

### Expected Result
Method name should be valid and truncated appropriately:
```java
@Preview(name = "This is a very long preview name with many words to test the method name generation")
public static DivTag userCard_this_is_a_very_long_preview_name_with_many_words_to_test_the_method_name_generation() {
    return userCard(new User("Test", "test@test.com"));
}
```

## Test Scenario 8: Quotes in Name

### Test Steps
1. Enter expression: `userCard(new User("Test", "test@test.com"))`
2. Click Save as @Preview (💾)
3. Enter name: `User says "Hello World"`
4. Click OK

### Expected Result
Quotes should be properly escaped in the annotation:
```java
@Preview(name = "User says \"Hello World\"")
public static DivTag userCard_user_says_hello_world() {
    return userCard(new User("Test", "test@test.com"));
}
```

## Test Scenario 9: Multi-line Expression

### Test Steps
1. Enter multi-line expression (with line breaks)
2. Click Execute (▶) to verify it works
3. Click Save as @Preview (💾)
4. Enter name: `Multi-line Test`
5. Click OK

### Expected Result
Expression should be preserved as entered, including formatting

## Test Scenario 10: Integration with Existing @Preview Methods

### Test Steps
1. Open `test-files/Phase5bPreviewAnnotationExample.java`
2. Select `customUserCard(String name, String email, String role) → DivTag`
3. Enter expression: `customUserCard("Diana", "diana@example.com", "Engineer")`
4. Click Execute (▶)
5. Click Save as @Preview (💾)
6. Enter name: `Diana the Engineer`
7. Click OK

### Expected Result
1. New method is added to the file
2. Both old and new @Preview methods appear in dropdown
3. New method appears with name "Diana the Engineer"
4. Old methods still work correctly

## UI Verification

### Button Layout
- Both Execute (▶) and Save as @Preview (💾) buttons should be visible
- Buttons should be side-by-side
- Buttons should have proper tooltips on hover
- Buttons should be disabled/enabled appropriately

### Dialog
- Dialog title: "Save as @Preview"
- Dialog message: "Enter a name for this preview:"
- OK and Cancel buttons present
- Text field accepts input

### Success Message
After saving, should show: "Preview method '[method_name]' created successfully!"

## Code Quality Checks

### Generated Code
- [ ] Proper indentation
- [ ] JavaDoc comment present
- [ ] @Preview annotation present
- [ ] Method is static
- [ ] Return type matches original method
- [ ] Method name is valid Java identifier
- [ ] Expression is properly terminated with semicolon
- [ ] No syntax errors

### File State
- [ ] File is marked as modified (asterisk in tab)
- [ ] Undo (Ctrl+Z) reverts the change
- [ ] File can be saved successfully
- [ ] No compilation errors in file

## Success Criteria

All test scenarios should:
1. ✅ Complete without errors
2. ✅ Generate valid Java code
3. ✅ Create methods that compile successfully
4. ✅ Create methods that execute in the preview
5. ✅ Preserve user's preview names accurately
6. ✅ Generate unique method names
7. ✅ Handle edge cases gracefully

## Known Issues / Limitations

1. Method names use simple lowercase+underscore conversion (not camelCase)
2. Methods are always added at the end of the class
3. No preview of generated code before insertion (must rely on undo)
4. No automatic import management
5. Backslashes in preview names are escaped (e.g., "Path\to\file" becomes "Path\\to\\file")

## Troubleshooting

### Button Not Visible
- Check that expression evaluator panel is visible
- Try selecting a parameterized method

### Method Not Created
- Check IntelliJ's Event Log for errors
- Verify file is not read-only
- Check that class exists in file

### Compilation Errors
- Ensure all required imports are present in the file
- Check that expression is valid Java syntax
- Verify return type matches

### Method Name Conflicts
- Plugin should auto-append numbers for duplicates
- If issues persist, manually rename existing method

## Reporting Issues

When reporting issues, please include:
1. IntelliJ IDEA version
2. Plugin version
3. Steps to reproduce
4. Expected vs actual behavior
5. Any error messages from Event Log
6. Generated code (if applicable)
