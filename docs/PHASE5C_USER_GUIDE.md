# Phase 5c User Guide: Save as @Preview

## Overview

The "Save as @Preview" feature allows you to convert temporary test expressions into permanent, reusable preview methods. This makes it easy to explore different variations of your j2html components and save the ones you like.

## When to Use This Feature

Use "Save as @Preview" when you:
- Find a good example while testing expressions
- Want to create multiple variations of a component
- Need to document different states of a component (empty, loading, error, etc.)
- Want to preserve specific test cases for future reference
- Are building a component library with examples

## Quick Start

### Step 1: Create an Expression
```java
// In the j2html Preview tool window
// 1. Select a method: userCard(User user) → DivTag
// 2. Type in expression editor:
userCard(new User("Alice", "alice@example.com"))
```

### Step 2: Test It
- Click the Execute button (▶)
- Verify the preview looks good

### Step 3: Save It
- Click the Save as @Preview button (💾)
- Enter a descriptive name: "Alice's Card"
- Click OK

### Step 4: Result
A new method is automatically created in your file:
```java
/**
 * Preview: Alice's Card
 */
@Preview(name = "Alice's Card")
public static DivTag userCard_alice() {
    return userCard(new User("Alice", "alice@example.com"));
}
```

The method now appears in your dropdown and can be viewed anytime!

## Example Workflows

### Workflow 1: Creating Component Variations

**Scenario**: You have a button component and want to document all its states

```java
// 1. Select: button(String text, String style) → ButtonTag

// 2. Create Primary Button
button("Click Me", "primary")
// Save as: "Button - Primary"

// 3. Create Secondary Button
button("Cancel", "secondary")
// Save as: "Button - Secondary"

// 4. Create Disabled Button
button("Disabled", "primary").withDisabled(true)
// Save as: "Button - Disabled"

// Result: Three preview methods in your dropdown!
```

### Workflow 2: Testing with Real Data

**Scenario**: Creating realistic user profile cards

```java
// 1. Select: userProfile(User user) → DivTag

// 2. Short Name
userProfile(new User("Ali", "ali@example.com"))
// Save as: "Profile - Short Name"

// 3. Long Name
userProfile(new User("Alexander the Great", "alexander@macedonia.com"))
// Save as: "Profile - Long Name Edge Case"

// 4. Special Characters
userProfile(new User("François José", "fj@example.com"))
// Save as: "Profile - Special Characters"
```

### Workflow 3: Component States

**Scenario**: Documenting form states

```java
// 1. Select: loginForm(boolean hasError, String errorMsg) → FormTag

// 2. Empty State
loginForm(false, "")
// Save as: "Login Form - Empty"

// 3. Error State
loginForm(true, "Invalid credentials")
// Save as: "Login Form - Error"

// 4. Success State (after modification)
loginForm(false, "Login successful!")
// Save as: "Login Form - Success"
```

## Best Practices

### Naming Your Previews

**Good Names:**
- Descriptive: "User Card - Premium Theme"
- State-based: "Button - Hover State"
- Data-based: "Product - Out of Stock"
- Use case: "Dashboard - Mobile View"

**Avoid:**
- Generic names: "Test", "Preview 1"
- Technical jargon: "userCard_v2_final_really"
- Too long: "This is a very long name that describes everything in detail..."

### Organizing Previews

Use consistent naming patterns to group related previews:

```
Card - Light Theme
Card - Dark Theme
Card - Compact Mode

Button - Primary
Button - Secondary
Button - Danger

Form - Empty
Form - Filled
Form - Error
```

### When NOT to Save

Don't save every expression you test:
- Syntax errors or failed attempts
- Temporary debugging expressions
- Duplicate examples (unless showing different states)
- Trivial variations that don't add value

## UI Reference

### Expression Evaluator Panel

```
┌────────────────────────────────────────────────────────┐
│ [Select method: userCard(User user) → DivTag       ▼] │
├────────────────────────────────────────────────────────┤
│ userCard(new User("Alice", "alice@ex..."))  [▶] [💾]  │
└────────────────────────────────────────────────────────┘
```

**Components:**
- **Dropdown**: Select which method to test
- **Expression Editor**: Type your Java expression
- **Execute (▶)**: Compile and preview the expression
- **Save as @Preview (💾)**: Save expression as a method

### Dialog Box

```
┌─────────────────────────────────────┐
│  Save as @Preview              [x]  │
├─────────────────────────────────────┤
│  Enter a name for this preview:     │
│  ┌─────────────────────────────┐   │
│  │ Alice's Card                │   │
│  └─────────────────────────────┘   │
│                                     │
│              [OK]  [Cancel]         │
└─────────────────────────────────────┘
```

## Generated Code Format

Every saved preview generates this structure:

```java
/**
 * Preview: [Your chosen name]
 */
@Preview(name = "[Your chosen name]")
public static [ReturnType] [methodName]() {
    return [your expression];
}
```

**Example:**
```java
/**
 * Preview: Alice's Card
 */
@Preview(name = "Alice's Card")
public static DivTag userCard_alice_s_card() {
    return userCard(new User("Alice", "alice@example.com"));
}
```

## Method Name Generation

The plugin automatically converts your preview name into a valid Java method name:

| Preview Name | Generated Method Name |
|--------------|----------------------|
| "Alice's Card" | `userCard_alice_s_card` |
| "User Profile - Dark" | `userCard_user_profile_dark` |
| "Button @ 100%" | `button_100` |
| "Product: $99.99" | `product_99_99` |

**Rules:**
1. Starts with the base method name
2. Converts preview name to lowercase
3. Replaces special characters with underscores
4. Removes duplicate underscores
5. Adds numbers if name already exists (`_test`, `_test1`, `_test2`)

## Tips and Tricks

### Tip 1: Iterate Quickly
1. Type expression → Execute → Preview
2. If it looks good → Save as @Preview
3. If not → Modify → Execute → Repeat

### Tip 2: Use Descriptive Names
Instead of "Test 1", use "User Card - Premium Account"
- Better documentation
- Easier to find later
- More professional

### Tip 3: Create a Preview Library
Save common patterns for reuse:
- Empty states
- Loading states
- Error states
- Edge cases (long text, special characters)

### Tip 4: Group by Feature
```java
// Navigation previews
@Preview(name = "Nav - Desktop")
@Preview(name = "Nav - Mobile")
@Preview(name = "Nav - Collapsed")

// Card previews
@Preview(name = "Card - Default")
@Preview(name = "Card - Hover")
@Preview(name = "Card - Selected")
```

### Tip 5: Don't Overdo It
- Quality over quantity
- Keep only meaningful examples
- Delete unused previews

## Keyboard Shortcuts

There are no built-in keyboard shortcuts yet, but you can use:
- `Tab` to move between fields
- `Enter` to click OK in dialog
- `Esc` to cancel dialog
- `Ctrl+Z` to undo if you make a mistake

## FAQ

### Q: Can I edit the generated method?
**A:** Yes! It's regular Java code. You can:
- Rename the method
- Modify the expression
- Change the @Preview annotation
- Add documentation

### Q: What happens if I use the same name twice?
**A:** The plugin automatically appends a number:
- First: `userCard_test`
- Second: `userCard_test1`
- Third: `userCard_test2`

### Q: Can I save multi-line expressions?
**A:** Yes! Just type the expression across multiple lines and save it.

### Q: What if I enter special characters in the name?
**A:** Special characters are converted to underscores in the method name, but preserved in the annotation.

### Q: Can I undo a save?
**A:** Yes! Use `Ctrl+Z` (Cmd+Z on Mac) to undo the method insertion.

### Q: Where are methods added?
**A:** At the end of the class. You can move them manually if needed.

### Q: Do I need imports?
**A:** All imports from the current file are available. If you need new imports, add them manually.

### Q: Can I save expressions from any method?
**A:** Only methods with parameters (that show the expression editor) can be saved.

## Troubleshooting

### Problem: Button not visible
**Solution:** Make sure you've selected a method with parameters. Zero-parameter methods don't need the expression editor.

### Problem: Dialog doesn't appear
**Solution:** Check that:
- You've selected a method
- The expression editor has text
- IntelliJ hasn't frozen

### Problem: Method not created
**Solution:** Check:
- File is not read-only
- You have write permissions
- No syntax errors in expression
- IntelliJ Event Log for errors

### Problem: Compilation errors
**Solution:**
- Verify imports are present
- Check expression syntax
- Ensure return type matches

## Advanced Usage

### Creating Test Suites
Use previews as visual test cases:
```java
@Preview(name = "Login - Valid Input")
@Preview(name = "Login - Invalid Email")
@Preview(name = "Login - Empty Fields")
@Preview(name = "Login - Server Error")
```

### Building Component Libraries
Document your components with examples:
```java
@Preview(name = "Button - Primary")
@Preview(name = "Button - Secondary")
@Preview(name = "Button - Danger")
@Preview(name = "Button - Large")
@Preview(name = "Button - Small")
```

### Responsive Design Testing
Test different viewports:
```java
@Preview(name = "Dashboard - Desktop (1920px)")
@Preview(name = "Dashboard - Tablet (768px)")
@Preview(name = "Dashboard - Mobile (375px)")
```

## Next Steps

1. **Explore**: Try different expressions and see what they render
2. **Save**: Keep the good ones as previews
3. **Organize**: Use consistent naming for easy navigation
4. **Share**: Your previews are in source code - team members see them too
5. **Maintain**: Update previews when components change

## Getting Help

If you encounter issues:
1. Check this guide
2. Review the testing guide in `PHASE5C_TESTING.md`
3. Check IntelliJ's Event Log (View → Tool Windows → Event Log)
4. Report issues on the project's issue tracker

---

**Happy Previewing! 🎨**
