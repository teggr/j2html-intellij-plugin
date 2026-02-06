# Phase 5c Visual Guide

## UI Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  j2html Preview Tool Window                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Select method: [userCard(User user) → DivTag            ▼]   │
│                                                                 │
│  ┌───────────────────────────────────────────────────┐  ┌───┐ │
│  │ userCard(new User("Alice", "alice@example.com"))  │  │ ▶ │ │
│  └───────────────────────────────────────────────────┘  └───┘ │
│                                                          ┌───┐ │
│                                                          │ 💾 │ │
│                                                          └───┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                                                         │  │
│  │              Preview Rendered Here                      │  │
│  │                                                         │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**Legend:**
- **▶** = Execute button (compile and preview)
- **💾** = Save as @Preview button (NEW!)

## Feature Flow Diagram

```
┌──────────────────┐
│  Start: User     │
│  has expression  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Click Execute   │
│  button (▶)      │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Preview shown   │
│  in tool window  │
└────────┬─────────┘
         │
         ▼
    ┌────────┐
    │ Looks  │
    │ good?  │
    └───┬────┘
        │
    Yes │        No
        │         └──────> Modify expression, try again
        │
        ▼
┌──────────────────┐
│  Click Save as   │
│  @Preview (💾)   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Dialog appears: │
│  "Enter name"    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  User enters:    │
│  "Alice's Card"  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Click OK        │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Plugin          │
│  generates code  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Method inserted │
│  into source     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Success message │
│  shown           │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Method appears  │
│  in dropdown     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  End: Permanent  │
│  preview created │
└──────────────────┘
```

## Code Generation Process

```
Input:
  • Current method: userCard(User user) → DivTag
  • Expression: userCard(new User("Alice", "alice@example.com"))
  • Preview name: "Alice's Card"

      ↓

Step 1: Generate Method Name
  • Base name: "userCard"
  • Preview name: "Alice's Card"
  • Sanitized: "alice_s_card"
  • Result: "userCard_alice_s_card"

      ↓

Step 2: Check Uniqueness
  • Get all methods in class
  • Check if "userCard_alice_s_card" exists
  • If exists, append number: "userCard_alice_s_card1"

      ↓

Step 3: Generate Code
  /**
   * Preview: Alice's Card
   */
  @Preview(name = "Alice's Card")
  public static DivTag userCard_alice_s_card() {
      return userCard(new User("Alice", "alice@example.com"));
  }

      ↓

Step 4: Insert into File
  • Use PSI to create method element
  • Add to containing class
  • Format with CodeStyleManager

      ↓

Step 5: Show Success
  • Message: "Preview method 'userCard_alice_s_card' created successfully!"
  • Method appears in dropdown as "Alice's Card"
```

## Method Name Conversion Examples

```
Preview Name              Generated Method Name
════════════════════════  ═══════════════════════════════════
"Alice's Card"         → userCard_alice_s_card
"User Profile - Dark"  → userCard_user_profile_dark
"Button @ 100%"        → button_button_100
"Product: $99.99"      → productDisplay_product_99_99
"Test"                 → userCard_test
"Test" (2nd)           → userCard_test1
"Test" (3rd)           → userCard_test2
"Hello World!"         → userCard_hello_world
"Login Form - Empty"   → loginForm_login_form_empty
```

## Special Character Handling

```
Character     Conversion    Example
═══════════   ═══════════   ═══════════════════════════
Space         _             "My Card" → "my_card"
Apostrophe    _             "Alice's" → "alice_s"
Dash          _             "Dark-Mode" → "dark_mode"
@             _             "@Preview" → "_preview"
$             _             "$99.99" → "_99_99"
:             _             "Name:Value" → "name_value"
()            _             "Test(1)" → "test_1_"
Multiple __   _             "A  B" → "a_b"
Leading _     removed       "_test" → "test"
Trailing _    removed       "test_" → "test"
```

## Dialog Box

```
┌────────────────────────────────────────────┐
│  Save as @Preview                    [x]   │
├────────────────────────────────────────────┤
│                                            │
│  Enter a name for this preview:            │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │ Alice's Card                   │░░│  │ │
│  └──────────────────────────────────────┘ │
│                                            │
│                     ┌────┐    ┌────────┐  │
│                     │ OK │    │ Cancel │  │
│                     └────┘    └────────┘  │
│                                            │
└────────────────────────────────────────────┘
```

**User Actions:**
- Type preview name
- Click OK to proceed
- Click Cancel to abort
- Press Enter to submit
- Press Esc to cancel

## Generated Code Structure

```java
// Generated by "Save as @Preview" feature

    /**
     * Preview: Alice's Card    ← JavaDoc with preview name
     */
    @Preview(name = "Alice's Card")    ← Annotation with display name
    public static DivTag userCard_alice_s_card() {    ← Method name
        return userCard(new User("Alice", "alice@example.com"));    ← Expression
    }
```

## Before and After

### Before Phase 5c:
```
User Experience:
  1. Type expression
  2. Click Execute
  3. See preview
  4. Change file
  5. Expression is lost ❌

Source File:
  • Only parameterized methods
  • No preview methods
  • Must manually create test cases
```

### After Phase 5c:
```
User Experience:
  1. Type expression
  2. Click Execute
  3. See preview
  4. Click Save as @Preview ✅
  5. Enter name
  6. Method created!
  7. Available forever ✅

Source File:
  • Parameterized methods
  • Generated preview methods
  • Automatic test case creation
  • Easy to maintain and reuse
```

## Integration with Phase 5b

```
Phase 5b:
  • @Preview annotation class
  • Display friendly names in dropdown
  • Example: @Preview(name = "Bootstrap Login Form")

Phase 5c:
  • Generate @Preview methods from expressions
  • Automatic method creation
  • Uses Phase 5b annotation

Together:
  ┌─────────────────────────────────────────┐
  │  Dropdown now shows:                    │
  ├─────────────────────────────────────────┤
  │  • Bootstrap Login Form     (Phase 5b)  │
  │  • Alice's Card             (Phase 5c)  │
  │  • Bob's Dark Card          (Phase 5c)  │
  │  • userCard(User) → DivTag  (no @Preview) │
  └─────────────────────────────────────────┘
```

## Component Architecture

```
PreviewPanel
├── UI Components
│   ├── Method Selector (dropdown)
│   ├── Expression Editor (text field)
│   └── Button Panel
│       ├── Execute Button (▶)
│       └── Save as @Preview Button (💾)  ← NEW!
│
├── Event Handlers
│   ├── onMethodSelected()
│   ├── executeExpression()
│   └── saveAsPreview()  ← NEW!
│
└── Helper Methods
    ├── generateMethodName()  ← NEW!
    ├── generatePreviewMethod()  ← NEW!
    └── insertPreviewMethod()  ← NEW!
```

## Success Message

```
┌────────────────────────────────────────────┐
│  ✓ Success                           [x]   │
├────────────────────────────────────────────┤
│                                            │
│  Preview method 'userCard_alice_s_card'    │
│  created successfully!                     │
│                                            │
│                              ┌────┐        │
│                              │ OK │        │
│                              └────┘        │
└────────────────────────────────────────────┘
```

## Error Scenarios

### Error: No Method Selected
```
┌────────────────────────────────────────┐
│  ⚠ Error                         [x]   │
├────────────────────────────────────────┤
│  No method selected                    │
│                         ┌────┐         │
│                         │ OK │         │
│                         └────┘         │
└────────────────────────────────────────┘
```

### Error: Expression is Empty
```
┌────────────────────────────────────────┐
│  ⚠ Error                         [x]   │
├────────────────────────────────────────┤
│  Expression is empty                   │
│                         ┌────┐         │
│                         │ OK │         │
│                         └────┘         │
└────────────────────────────────────────┘
```

## File Changes

### Before:
```java
public class Phase5ExampleWithObjects {
    
    public static DivTag userCard(User user) {
        return div()
            .withClass("user-card")
            .with(
                h2(user.name),
                p(user.email)
            );
    }
}
```

### After (with saved preview):
```java
public class Phase5ExampleWithObjects {
    
    public static DivTag userCard(User user) {
        return div()
            .withClass("user-card")
            .with(
                h2(user.name),
                p(user.email)
            );
    }
    
    /**
     * Preview: Alice's Card
     */
    @Preview(name = "Alice's Card")
    public static DivTag userCard_alice_s_card() {
        return userCard(new User("Alice", "alice@example.com"));
    }
}
```

## Keyboard Navigation

```
Dialog Box:
  Tab       → Move focus
  Enter     → Submit (OK)
  Esc       → Cancel
  Type text → Enter preview name

Main Window:
  Mouse     → Click buttons
  Ctrl+Z    → Undo method insertion
  Ctrl+S    → Save file
```

## Feature Summary

```
┌─────────────────────────────────────────────┐
│  Phase 5c: Save as @Preview                 │
├─────────────────────────────────────────────┤
│  ✅ Save button (💾)                         │
│  ✅ Name prompt dialog                       │
│  ✅ Method name generation                   │
│  ✅ Code generation                          │
│  ✅ PSI insertion                            │
│  ✅ Uniqueness checking                      │
│  ✅ String escaping                          │
│  ✅ Code formatting                          │
│  ✅ Error handling                           │
│  ✅ Success notification                     │
│  ✅ Dropdown integration                     │
│  ✅ Documentation                            │
└─────────────────────────────────────────────┘
```

---

**Visual guide created to complement the technical documentation and user guide.**
