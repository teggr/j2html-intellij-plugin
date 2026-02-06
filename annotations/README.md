# j2html Preview Annotations

This module provides annotations for marking j2html component preview methods that can be used by the j2html Preview IntelliJ plugin.

## Purpose

The `@Preview` annotation allows you to mark methods as preview examples that will be displayed with friendly names in the j2html Preview plugin's dropdown, rather than showing the full method signature.

## Usage

Add the dependency to your project:

### Maven

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>j2html-preview-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation 'com.example:j2html-preview-annotations:0.1.0-SNAPSHOT'
}
```

### Gradle Kotlin DSL

```kotlin
dependencies {
    implementation("com.example:j2html-preview-annotations:0.1.0-SNAPSHOT")
}
```

## Example

```java
import com.example.j2htmlpreview.Preview;
import j2html.tags.specialized.DivTag;
import static j2html.TagCreator.*;

public class MyComponents {
    
    // Regular component method
    public static DivTag userCard(User user, String theme) {
        return div(
            h2(user.getName()),
            p(user.getEmail())
        ).withClass("card", theme);
    }
    
    // Preview method with friendly display name
    @Preview(name = "User Card - Alice (Dark Theme)")
    public static DivTag userCard_alice_dark() {
        User alice = new User("Alice", "alice@example.com");
        return userCard(alice, "dark");
    }
    
    // Another preview with different data
    @Preview(name = "User Card - Bob (Light Theme)")
    public static DivTag userCard_bob_light() {
        User bob = new User("Bob", "bob@example.com");
        return userCard(bob, "light");
    }
}
```

## Annotation Attributes

### Required

- **`name`**: The friendly display name that will be shown in the preview dropdown

### Optional

- **`description`**: A description of what this preview demonstrates (reserved for future use)
- **`tags`**: Array of tags for categorization (reserved for future use)

## Notes

- This is a lightweight annotation library with no dependencies
- The annotation has `@Retention(RUNTIME)` so it's available via reflection
- The annotation is targeted at methods only (`@Target(ElementType.METHOD)`)
- In the future, build-time stripping plugins may be provided to remove preview methods from production builds

## License

Apache License 2.0
