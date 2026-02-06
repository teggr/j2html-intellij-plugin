# Example: Using j2html Preview Annotations in Your Project

This example shows how to use the `@Preview` annotation in your own project.

## Project Setup

### Step 1: Add the Dependency

Add the j2html-preview-annotations dependency to your project's build file.

#### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    // j2html library
    implementation("com.j2html:j2html:1.6.0")
    
    // Preview annotations (for marking preview methods)
    implementation("com.example:j2html-preview-annotations:0.1.0-SNAPSHOT")
}
```

#### Gradle (Groovy)

```groovy
// build.gradle
dependencies {
    implementation 'com.j2html:j2html:1.6.0'
    implementation 'com.example:j2html-preview-annotations:0.1.0-SNAPSHOT'
}
```

#### Maven

```xml
<dependencies>
    <!-- j2html library -->
    <dependency>
        <groupId>com.j2html</groupId>
        <artifactId>j2html</artifactId>
        <version>1.6.0</version>
    </dependency>
    
    <!-- Preview annotations -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>j2html-preview-annotations</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Step 2: Create Your Components

Create your j2html components with preview methods:

```java
package com.mycompany.components;

import com.example.j2htmlpreview.Preview;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import static j2html.TagCreator.*;

public class UserComponents {
    
    /**
     * Reusable user card component
     */
    public static DivTag userCard(String name, String email, String role) {
        return div()
            .withClass("user-card")
            .with(
                h2(name),
                p(email),
                span(role).withClass("role-badge")
            );
    }
    
    /**
     * Preview: User card for a regular user
     * This will appear in the dropdown as "User Card - Regular User"
     */
    @Preview(name = "User Card - Regular User")
    public static DivTag userCard_regular() {
        return userCard("John Doe", "john@example.com", "User");
    }
    
    /**
     * Preview: User card for an admin
     * This will appear as "User Card - Administrator"
     */
    @Preview(name = "User Card - Administrator")
    public static DivTag userCard_admin() {
        return userCard("Jane Admin", "jane@example.com", "Admin");
    }
    
    /**
     * Preview: User card with a long name (edge case testing)
     */
    @Preview(
        name = "User Card - Long Name Edge Case",
        description = "Tests how the card handles very long names",
        tags = {"edge-case", "layout-test"}
    )
    public static DivTag userCard_longName() {
        return userCard(
            "Dr. Bartholomew Alexander Montgomery III, Esq.",
            "bartholomew.montgomery@example.com",
            "Distinguished Professor"
        );
    }
}
```

### Step 3: Create Form Components

```java
package com.mycompany.components;

import com.example.j2htmlpreview.Preview;
import j2html.tags.specialized.FormTag;
import static j2html.TagCreator.*;

public class FormComponents {
    
    /**
     * Reusable login form
     */
    public static FormTag loginForm(boolean showErrors, String errorMessage) {
        return form()
            .withMethod("post")
            .withClass("login-form")
            .with(
                h2("Sign In"),
                showErrors ? div(errorMessage).withClass("error-message") : null,
                div(
                    label("Email").withFor("email"),
                    input().withType("email").withId("email").withName("email")
                ).withClass("form-group"),
                div(
                    label("Password").withFor("password"),
                    input().withType("password").withId("password").withName("password")
                ).withClass("form-group"),
                button("Sign In").withType("submit").withClass("btn-primary")
            );
    }
    
    /**
     * Preview: Login form in clean state
     */
    @Preview(name = "Login Form - Clean State")
    public static FormTag loginForm_clean() {
        return loginForm(false, null);
    }
    
    /**
     * Preview: Login form with validation error
     */
    @Preview(name = "Login Form - With Error")
    public static FormTag loginForm_error() {
        return loginForm(true, "Invalid email or password. Please try again.");
    }
}
```

### Step 4: Use in IntelliJ IDEA

1. **Install the Plugin**: Install the j2html Preview plugin in IntelliJ IDEA
2. **Open Your File**: Open any file containing preview methods
3. **Open Preview Window**: View → Tool Windows → j2html Preview
4. **Select a Preview**: Use the dropdown to select a preview by its friendly name
5. **View the Render**: See the HTML output rendered in the preview pane

## Project Structure

```
my-project/
├── build.gradle.kts (or pom.xml)
└── src/
    └── main/
        └── java/
            └── com/
                └── mycompany/
                    └── components/
                        ├── UserComponents.java
                        └── FormComponents.java
```

## Benefits

### For Development
- **Visual Testing**: See your components rendered immediately
- **Rapid Iteration**: Make changes and see results instantly
- **Example Documentation**: Preview methods serve as living examples
- **Edge Case Testing**: Create previews for different states and edge cases

### For Team Collaboration
- **Shared Examples**: Preview methods show teammates how to use components
- **Design Review**: Designers can see components without running the full app
- **Component Library**: Build a catalog of reusable components with examples

## Best Practices

### Naming Conventions

Use descriptive preview names that explain what's being shown:

```java
// Good
@Preview(name = "Button - Primary (Enabled)")
@Preview(name = "Button - Primary (Disabled)")
@Preview(name = "Button - Secondary")

// Not as good
@Preview(name = "Button 1")
@Preview(name = "Button 2")
@Preview(name = "Another Button")
```

### Organization

Group related previews together:

```java
@Preview(name = "Alert - Success")
@Preview(name = "Alert - Warning")
@Preview(name = "Alert - Error")
@Preview(name = "Alert - Info")
```

### Descriptions and Tags

Use optional metadata for better organization:

```java
@Preview(
    name = "Product Card - Featured",
    description = "Product card with featured badge and promotional pricing",
    tags = {"product", "featured", "promotion"}
)
```

### Self-Contained Examples

Make preview methods self-contained - they should not require external setup:

```java
// Good - self-contained
@Preview(name = "User Profile - Complete")
public static DivTag userProfile_complete() {
    User user = new User("Alice", "alice@example.com", "Software Engineer");
    user.setBio("Passionate about clean code and good design");
    user.setAvatarUrl("https://example.com/avatar.jpg");
    return userProfile(user);
}

// Not ideal - requires external setup
@Preview(name = "User Profile")
public static DivTag userProfile_needsSetup() {
    User user = getUserFromDatabase(); // External dependency
    return userProfile(user);
}
```

## Scope Configuration

If you only want the annotation in development (not in production), use different scopes:

### Gradle
```kotlin
dependencies {
    // Only in dev/test, not in production
    compileOnly("com.example:j2html-preview-annotations:0.1.0-SNAPSHOT")
    
    // Or use testImplementation for test-only previews
    testImplementation("com.example:j2html-preview-annotations:0.1.0-SNAPSHOT")
}
```

### Maven
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>j2html-preview-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>provided</scope> <!-- Not included in final JAR -->
</dependency>
```

## See Also

- [j2html Documentation](https://j2html.com/)
- [Main Plugin README](../../README.md)
- [Annotations Module README](../../annotations/README.md)
