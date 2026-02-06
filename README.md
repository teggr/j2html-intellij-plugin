# j2html Preview Plugin

An IntelliJ IDEA plugin for previewing j2html components with live rendering.

## Project Structure

This project is organized as a multi-module Gradle build:

- **`annotations`**: A standalone Java library containing the `@Preview` annotation
  - Can be used independently in any Java project
  - No dependencies on IntelliJ Platform
  - Can be published to Maven Central for easy consumption
  - See [annotations/README.md](annotations/README.md) for usage details

- **`plugin`**: The IntelliJ IDEA plugin
  - Provides the preview tool window and UI
  - Detects and displays j2html methods
  - Executes methods and renders HTML
  - Depends on the annotations module

### Why Multi-Module?

The annotation needs to be available as a dependency in user projects so they can mark their preview methods. By separating it into its own module, users can add just the lightweight annotation library without needing the entire plugin.

## Current Status: Phase 5b ✅

- ✅ Basic tool window with static HTML preview (Phase 1)
- ✅ Detect current Java file (Phase 2)
- ✅ Find j2html methods (Phase 3)
- ✅ Execute and render (Phase 4)
- ✅ **@Preview annotation for friendly names (Phase 5b)** ← NEW!
- ⏳ Preview providers with parameters (Phase 5c)
- ⏳ Live updates (Phase 6)

## Phase 5b Features

### 🏷️ @Preview Annotation Support
- **Friendly Display Names**: Show descriptive names instead of method signatures in dropdown
- **Type-Safe**: Uses Java annotation for compile-time checking
- **Backward Compatible**: Non-annotated methods continue to work with signature display
- **PSI Integration**: Detects annotations at design time using IntelliJ's PSI

### Example Usage
```java
import com.example.j2htmlpreview.Preview;

// Regular method - shows: userCard(User user) → DivTag
public static DivTag userCard(User user) {
    return div(h2(user.name), p(user.email));
}

// Preview method - shows: User Card - Alice
@Preview(name = "User Card - Alice")
public static DivTag userCard_alice() {
    return userCard(new User("Alice", "alice@example.com"));
}
```

### Dropdown Display
**Before**: `bootstrapForm() → ContainerTag`  
**After**: `Bootstrap Login Form` ← Clean, descriptive name

## Phase 4 Features

### 🚀 Method Execution via Reflection
- **Execute Static Methods**: Runs static methods with zero parameters using Java reflection
- **Module ClassLoader**: Builds custom classloader with full module dependencies including j2html
- **Runtime Invocation**: Bridges PSI (static analysis) to runtime execution
- **HTML Rendering**: Automatically calls `.render()` on j2html objects to get HTML output

### 💎 Professional UI
- **Success Display**: Green banner with styled output container showing rendered HTML
- **Error Handling**: Clear, helpful error messages for all failure cases
- **Method Info**: Shows which method was executed above the rendered output

### 🛡️ Robust Error Handling
- Non-static methods → Clear error message
- Methods with parameters → "Parameter handling will be added in Phase 5"
- Uncompiled code → "Make sure the project is compiled"
- Null returns, exceptions, and other edge cases handled gracefully

### Phase 4a Scope (Currently Implemented)
- ✅ Static methods with zero parameters
- ❌ Non-static methods (Phase 5+)
- ❌ Methods with parameters (Phase 5)

## Phase 3 Features
- **PSI-Based Method Detection**: Uses IntelliJ's Program Structure Interface to analyze Java code
- **Method Discovery**: Automatically finds methods that return j2html types (ContainerTag, DomContent, Tag, etc.)
- **Method Selector Dropdown**: Interactive dropdown showing all discovered j2html methods with their signatures
- **Method Signature Display**: Shows method name, parameters with types, and return type
- **Context-Aware Messages**: Different HTML previews for different scenarios (methods found, no methods, non-Java files, etc.)
- **File Detection**: Automatically detects the currently open file in the editor
- **Live Updates**: Preview updates automatically when you switch between files
- **Event-Driven Architecture**: Uses IntelliJ's MessageBus system to listen for file changes

## Development

### Prerequisites
- JDK 17 or later
- IntelliJ IDEA (for development)

### Building

The project uses Gradle with a multi-module structure. To build all modules:

```bash
./gradlew build
```

This will:
1. Build the `annotations` module (creates JAR with sources and javadoc)
2. Build the `plugin` module (creates the IntelliJ plugin ZIP)

#### Building Individual Modules

Build just the annotations library:
```bash
./gradlew :annotations:build
```

Build just the plugin:
```bash
./gradlew :plugin:build
```

### Running the Plugin

To test the plugin in a sandboxed IntelliJ instance:

```bash
./gradlew :plugin:runIde
```

This will launch a new IntelliJ instance with the plugin installed.

### Publishing the Annotations Library

The annotations module is configured with maven-publish plugin. To publish to a local Maven repository:

```bash
./gradlew :annotations:publishToMavenLocal
```

The distributable plugin will be in `plugin/build/distributions/`.

### Using the Annotations in Your Project

Add the annotations dependency to your project's build file:

**Gradle:**
```groovy
dependencies {
    implementation 'com.example:j2html-preview-annotations:0.1.0-SNAPSHOT'
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>j2html-preview-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Then use the annotation in your code:
```java
import com.example.j2htmlpreview.Preview;

@Preview(name = "My Preview Example")
public static ContainerTag myPreview() {
    return div("Hello World");
}
```

## About j2html

[j2html](https://j2html.com/) is a Java library for generating HTML in a typesafe way.
