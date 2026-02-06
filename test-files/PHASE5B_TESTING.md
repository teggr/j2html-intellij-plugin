# Phase 5b Testing Guide: @Preview Annotation

## Overview
This guide provides step-by-step instructions for testing the new @Preview annotation feature that allows methods to have friendly display names in the dropdown.

## Test Files

### 1. Phase5bPreviewAnnotationExample.java
- **Location**: `test-files/Phase5bPreviewAnnotationExample.java`
- **Purpose**: Comprehensive test file with various @Preview annotation scenarios
- **Test Cases**:
  - Regular method without annotation
  - Multiple methods with friendly @Preview names
  - Methods with parameters that have @Preview
  - Method with empty annotation name (fallback test)
  - Mixed annotated and non-annotated methods

### 2. ExampleJ2HtmlComponents.java (Updated)
- **Location**: `test-files/ExampleJ2HtmlComponents.java`
- **Purpose**: Existing test file now with some @Preview annotations added
- **Test Cases**:
  - Existing methods without annotations (backward compatibility)
  - New preview methods with friendly names

## Running the Plugin

### Start the Plugin
```bash
cd /home/runner/work/j2html-intellij-plugin/j2html-intellij-plugin
./gradlew runIde
```

This will launch IntelliJ IDEA with the plugin installed.

## Test Scenarios

### Test 1: Basic @Preview Annotation
**File**: `Phase5bPreviewAnnotationExample.java`

1. Open the test file in the IDE
2. Open the "j2html Preview" tool window
3. Check the dropdown list

**Expected Results**:
- `simpleCard() → DivTag` (no annotation)
- `Bootstrap Login Form` (from @Preview annotation)
- `Alice's User Card` (from @Preview annotation)
- `User Card - Long Name Edge Case` (from @Preview annotation)

### Test 2: Execute Annotated Method
**File**: `Phase5bPreviewAnnotationExample.java`

1. Select "Bootstrap Login Form" from dropdown
2. Click execute or press Enter

**Expected Results**:
- Method executes successfully
- HTML output displays the login form
- Success banner shows "Bootstrap Login Form" as the method name

### Test 3: Mixed Annotated and Non-Annotated
**File**: `ExampleJ2HtmlComponents.java`

1. Open the file in the IDE
2. Check the dropdown list

**Expected Results**:
- Old methods show: `simpleComponent() → ContainerTag`
- Old methods show: `loginForm() → DomContent`
- Old methods show: `userCard(String name, String email) → ContainerTag`
- New annotated shows: `Hello World Example`
- New annotated shows: `Simple Login Form`

### Test 4: Execute Methods in Mixed File
**File**: `ExampleJ2HtmlComponents.java`

1. Select and execute `simpleComponent()` (non-annotated)
2. Select and execute `Hello World Example` (annotated)

**Expected Results**:
- Both execute correctly
- Non-annotated shows method signature in output
- Annotated shows friendly name in output

### Test 5: Empty Name Fallback
**File**: `Phase5bPreviewAnnotationExample.java`

1. Find method `emptyNameTest()` with `@Preview(name = "")`
2. Check dropdown display

**Expected Results**:
- Should show: `emptyNameTest() → DivTag` (fallback to method signature)

### Test 6: Annotated Method with Parameters
**File**: `Phase5bPreviewAnnotationExample.java`

1. Select "Custom User Card Template" from dropdown
2. Try to execute

**Expected Results**:
- Dropdown shows: `Custom User Card Template` (friendly name)
- Execution shows error: "This method requires parameters..." (Phase 4 limitation)

### Test 7: Long Display Names
**File**: `Phase5bPreviewAnnotationExample.java`

1. Check dropdown display for all methods

**Expected Results**:
- Long names like "User Card - Long Name Edge Case" display correctly
- Dropdown doesn't truncate or break with long names
- UI remains readable

### Test 8: Multiple Preview Methods
**File**: `Phase5bPreviewAnnotationExample.java`

1. Open the file
2. Count methods in dropdown

**Expected Results**:
- All valid j2html methods are listed
- Annotated ones show friendly names
- Non-annotated show method signatures
- Order is preserved

### Test 9: File Switching
1. Open `Phase5bPreviewAnnotationExample.java`
2. Switch to `ExampleJ2HtmlComponents.java`
3. Switch back to `Phase5bPreviewAnnotationExample.java`

**Expected Results**:
- Dropdown updates correctly on file switch
- Friendly names persist and display correctly
- No errors or UI glitches

### Test 10: Execution Flow End-to-End
**File**: `Phase5bPreviewAnnotationExample.java`

1. Select "Product Card - In Stock"
2. Execute
3. Select "Product Card - Out of Stock"
4. Execute
5. Select `simpleCard()` (no annotation)
6. Execute

**Expected Results**:
- All three execute successfully
- Output shows correct HTML for each
- Friendly names appear in success banners for annotated methods
- Method signature appears for non-annotated method

## Validation Checklist

- [ ] Plugin builds without errors
- [ ] Plugin starts with `./gradlew runIde`
- [ ] Test files load without compilation errors
- [ ] @Preview annotations detected correctly
- [ ] Friendly names displayed in dropdown
- [ ] Non-annotated methods show signatures
- [ ] Annotated methods execute correctly
- [ ] Empty name falls back to signature
- [ ] Mixed files work correctly
- [ ] File switching preserves functionality
- [ ] No console errors or exceptions
- [ ] UI remains responsive and readable

## Expected Dropdown Output

### Phase5bPreviewAnnotationExample.java
```
Select method:
  • simpleCard() → DivTag
  • Bootstrap Login Form
  • Alice's User Card
  • User Card - Long Name Edge Case
  • Product Card - In Stock
  • Product Card - Out of Stock
  • Custom User Card Template
  • buildCard(String title, String content) → ContainerTag
  • emptyNameTest() → DivTag
```

### ExampleJ2HtmlComponents.java
```
Select method:
  • simpleComponent() → ContainerTag
  • userCard(String name, String email) → ContainerTag
  • loginForm() → DomContent
  • container(DomContent... content) → DivTag
  • productCard(String name, double price, String imageUrl, boolean inStock) → ContainerTag
  • Hello World Example
  • Simple Login Form
```

## Known Limitations (Expected Behavior)

1. **Parameters**: Methods with @Preview and parameters show friendly names but still show error on execution (Phase 4 limitation)
2. **Empty Names**: @Preview with empty string falls back to method signature
3. **Compilation**: Annotation must be compiled/available on classpath to be detected

## Success Criteria

✅ All test scenarios pass
✅ No runtime errors or exceptions
✅ UI displays friendly names correctly
✅ Backward compatibility maintained
✅ Code builds and runs successfully

## Troubleshooting

### Friendly Names Not Showing
- Ensure annotation is imported: `import com.example.j2htmlpreview.Preview;`
- Check annotation syntax: `@Preview(name = "Display Name")`
- Verify project is compiled

### Annotation Not Found
- Copy `Preview.java` to project being tested
- Ensure it's in package: `com.example.j2htmlpreview`

### UI Not Updating
- Close and reopen the tool window
- Switch to another file and back
- Rebuild project: Build → Rebuild Project
