# Local Maven Installation Guide

## Overview
This guide explains how to install the j2html-preview-annotations library to your local Maven repository so it can be referenced by other projects.

## Date
February 6, 2026

## Quick Start for Maven Users

**Step 1:** Install to local Maven repository:
```cmd
gradlew.bat :annotations:publishToMavenLocal
```

**Step 2:** Add to your `pom.xml`:
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>j2html-preview-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

**Step 3:** Build your project:
```cmd
mvn clean compile
```

That's it! The annotations are now available in your Maven project.

---

## Installation Steps

### 1. Publish to Local Maven Repository

From the project root directory, run:

```cmd
gradlew.bat :annotations:publishToMavenLocal
```

Or if you're already in the project directory:

```cmd
.\gradlew.bat :annotations:publishToMavenLocal
```

This command will:
- Build the annotations module
- Generate the JAR file
- Generate sources JAR
- Generate Javadoc JAR
- Publish all artifacts to your local Maven repository

### 2. Verify Installation

After successful execution, the library will be installed to:

```
C:\Users\robin\.m2\repository\com\example\j2html-preview-annotations\0.1.0-SNAPSHOT\
```

The directory should contain:
- `j2html-preview-annotations-0.1.0-SNAPSHOT.jar` - Main library
- `j2html-preview-annotations-0.1.0-SNAPSHOT-sources.jar` - Source code
- `j2html-preview-annotations-0.1.0-SNAPSHOT-javadoc.jar` - Javadoc
- `j2html-preview-annotations-0.1.0-SNAPSHOT.pom` - POM file
- Maven metadata files

## Maven Coordinates

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>j2html-preview-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Using in Other Projects

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenLocal()  // Add this to use locally installed artifacts
    mavenCentral()
}

dependencies {
    implementation("com.example:j2html-preview-annotations:0.1.0-SNAPSHOT")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.example:j2html-preview-annotations:0.1.0-SNAPSHOT'
}
```

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependencies>
    <!-- Add this dependency -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>j2html-preview-annotations</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Your other dependencies -->
</dependencies>
```

**Complete pom.xml example:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.yourcompany</groupId>
    <artifactId>your-project</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- j2html Preview Annotations -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>j2html-preview-annotations</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>

        <!-- j2html library (if not already included) -->
        <dependency>
            <groupId>com.j2html</groupId>
            <artifactId>j2html</artifactId>
            <version>1.6.0</version>
        </dependency>
    </dependencies>

</project>
```

**Important Notes:**
- Maven automatically checks the local repository (`~/.m2/repository`) by default - no repository configuration needed
- After adding the dependency, run `mvn clean compile` to download and compile
- If you get "Cannot resolve dependency" error, make sure you've run the installation command first

## Build Configuration Details

The annotations module is configured with:
- **Group ID**: `com.example`
- **Artifact ID**: `j2html-preview-annotations`
- **Version**: `0.1.0-SNAPSHOT`
- **Java Version**: 17
- **Includes**: Main JAR, Sources JAR, Javadoc JAR

## Troubleshooting

### Issue: Build Fails

If the build fails, try:
1. Clean the build first: `gradlew.bat clean`
2. Then publish: `gradlew.bat :annotations:publishToMavenLocal`

### Issue: Cannot Find Artifact

1. Verify the local Maven repository path:
   - Windows: `C:\Users\robin\.m2\repository`
   - Linux/Mac: `~/.m2/repository`

2. Check that the artifact was published:
   ```cmd
   dir C:\Users\robin\.m2\repository\com\example\j2html-preview-annotations\0.1.0-SNAPSHOT
   ```

3. Ensure `mavenLocal()` is in your repositories list

### Issue: Outdated Snapshot

SNAPSHOT versions are mutable. To force an update:
- Gradle: `gradlew.bat build --refresh-dependencies`
- Maven: `mvn clean install -U`

## Updating the Library

After making changes to the annotations module:
1. Make your code changes
2. Run `gradlew.bat :annotations:publishToMavenLocal` again
3. Refresh dependencies in consuming projects

## Next Steps

After installation, you can:
1. Use the annotations in your j2html projects
2. Share the installation instructions with team members
3. Consider publishing to a remote Maven repository for team-wide access

## Related Files

- Build configuration: `/annotations/build.gradle.kts`
- Annotation source code: `/annotations/src/main/java/`
- Annotations README: `/annotations/README.md`
