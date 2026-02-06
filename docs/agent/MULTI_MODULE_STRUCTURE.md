# Multi-Module Project Structure

## Overview

This document describes the multi-module Gradle project structure for the j2html Preview plugin.

## Problem Statement

The `@Preview` annotation needs to be usable in user projects that don't have a direct reference to the IntelliJ plugin. Users need to be able to add the annotation as a dependency to their projects, similar to how IntelliJ's own annotations (like `@NotNull`, `@Nullable`) are available as standalone dependencies.

## Solution

The project has been restructured as a multi-module Gradle build with two modules:

### Module 1: `annotations`

A standalone Java library containing only the `@Preview` annotation.

**Location**: `annotations/`

**Key Features**:
- Pure Java library with no dependencies
- No IntelliJ Platform dependencies
- Includes `@Retention(RUNTIME)` for reflection access
- Configured for Maven publishing
- Generates sources JAR and Javadoc JAR

**Build Artifacts**:
- `annotations-0.1.0-SNAPSHOT.jar` - Main JAR with annotation class
- `annotations-0.1.0-SNAPSHOT-sources.jar` - Sources for IDE integration
- `annotations-0.1.0-SNAPSHOT-javadoc.jar` - Javadoc for documentation

**Publishing**:
```bash
./gradlew :annotations:publishToMavenLocal
```

### Module 2: `plugin`

The IntelliJ IDEA plugin that provides the preview functionality.

**Location**: `plugin/`

**Key Features**:
- Depends on the `annotations` module
- Contains all IntelliJ plugin code
- Uses PSI to detect `@Preview` annotations
- Provides the preview tool window UI

**Build Artifacts**:
- `plugin-0.1.0-SNAPSHOT.zip` - Installable IntelliJ plugin

**Running**:
```bash
./gradlew :plugin:runIde
```

## Project Layout

```
j2html-intellij-plugin/
├── annotations/                    # Annotation library module
│   ├── build.gradle.kts           # Annotations build config
│   ├── README.md                  # Usage documentation
│   └── src/
│       └── main/
│           └── java/
│               └── com/example/j2htmlpreview/
│                   └── Preview.java
│
├── plugin/                        # IntelliJ plugin module
│   ├── build.gradle.kts          # Plugin build config
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/example/j2htmlpreview/
│       │   │       ├── J2HtmlPreviewToolWindowFactory.java
│       │   │       └── PreviewPanel.java
│       │   └── resources/
│       │       └── META-INF/
│       │           └── plugin.xml
│       └── test/                  # Future: plugin tests
│
├── build.gradle.kts               # Root build config
├── settings.gradle.kts            # Multi-module configuration
└── README.md                      # Main documentation
```

## Build Configuration

### Root `build.gradle.kts`

Defines common configuration and applies plugins to submodules:

```kotlin
plugins {
    id("org.jetbrains.intellij") version "1.17.4" apply false
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

subprojects {
    repositories {
        mavenCentral()
    }
}
```

### `annotations/build.gradle.kts`

Standard Java library with Maven publishing:

```kotlin
plugins {
    id("java")
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "j2html-preview-annotations"
            // ... POM metadata
        }
    }
}
```

### `plugin/build.gradle.kts`

IntelliJ plugin configuration with dependency on annotations:

```kotlin
plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

dependencies {
    implementation(project(":annotations"))
    implementation("com.j2html:j2html:1.6.0")
}

intellij {
    version.set("2024.1")
    type.set("IC")
    plugins.set(listOf("java"))
}
```

### `settings.gradle.kts`

Includes both modules:

```kotlin
rootProject.name = "j2html-preview-plugin"

include("annotations")
include("plugin")
```

## Usage for End Users

### 1. Add Dependency

Users add the annotations library to their project:

**Gradle**:
```kotlin
dependencies {
    implementation("com.example:j2html-preview-annotations:0.1.0-SNAPSHOT")
}
```

**Maven**:
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>j2html-preview-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Use the Annotation

```java
import com.example.j2htmlpreview.Preview;
import static j2html.TagCreator.*;

public class MyComponents {
    @Preview(name = "User Card - Example")
    public static DivTag userCardExample() {
        return div(h2("Alice"), p("alice@example.com"));
    }
}
```

### 3. Install Plugin

Users install the plugin in IntelliJ IDEA, which can then detect and display the `@Preview` annotations.

## Build Commands

### Build Everything
```bash
./gradlew build
```

### Build Annotations Only
```bash
./gradlew :annotations:build
```

### Build Plugin Only
```bash
./gradlew :plugin:build
```

### Publish Annotations to Local Maven
```bash
./gradlew :annotations:publishToMavenLocal
```

### Run Plugin in Development
```bash
./gradlew :plugin:runIde
```

### Clean Everything
```bash
./gradlew clean
```

## Benefits of Multi-Module Structure

1. **Separation of Concerns**: Annotation library is independent of plugin code
2. **Lightweight Dependency**: Users only need the small annotation JAR, not the plugin
3. **Better Publishing**: Annotations can be published to Maven Central independently
4. **Type Safety**: Users get compile-time checking when using `@Preview`
5. **No Runtime Dependency**: Annotation has no dependencies, making it safe to include
6. **Future Flexibility**: Could add more modules (e.g., Maven plugin for stripping)

## Migration Notes

The old single-module structure has been completely replaced:

- **Before**: All code in `src/main/java`
- **After**: Code split between `annotations/src/main/java` and `plugin/src/main/java`

The plugin distribution (`plugin-0.1.0-SNAPSHOT.zip`) automatically includes the annotations JAR, so plugin users get both components.

## Future Enhancements

Possible additional modules:

1. **`gradle-plugin`**: Gradle plugin for build-time stripping of preview methods
2. **`maven-plugin`**: Maven plugin for the same purpose
3. **`documentation-generator`**: Tool to generate documentation from `@Preview` methods
4. **`test-utils`**: Testing utilities for preview methods

## License

Apache License 2.0 - Same as the main project
