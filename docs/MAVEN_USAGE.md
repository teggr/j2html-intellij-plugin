# Using j2html-preview-annotations in Maven Projects

## Prerequisites

Before you can use the annotations in your Maven project, you need to install them to your local Maven repository.

## Step-by-Step Guide

### 1. Install to Local Maven Repository

From the j2html-intellij-plugin project root:

```cmd
gradlew.bat :annotations:publishToMavenLocal
```

This installs the library to `C:\Users\robin\.m2\repository\com\example\j2html-preview-annotations\0.1.0-SNAPSHOT\`

### 2. Add Dependency to Your Maven Project

Open your project's `pom.xml` and add the dependency:

```xml
<dependencies>
    <!-- j2html Preview Annotations -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>j2html-preview-annotations</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- j2html (if not already present) -->
    <dependency>
        <groupId>com.j2html</groupId>
        <artifactId>j2html</artifactId>
        <version>1.6.0</version>
    </dependency>
</dependencies>
```

### 3. Verify the Dependency

Reload your Maven project (in IntelliJ IDEA: right-click on pom.xml → Maven → Reload project)

Or from command line:
```cmd
mvn clean compile
```

### 4. Use the Annotations

Create a Java class with preview methods:

```java
import com.example.j2html.preview.annotations.Preview;
import static j2html.TagCreator.*;

public class MyComponents {
    
    @Preview
    public static ContainerTag<?> buttonPreview() {
        return button("Click Me")
                .withClass("btn btn-primary");
    }
    
    @Preview
    public static ContainerTag<?> cardPreview() {
        return div(
            h2("Card Title"),
            p("Card content goes here")
        ).withClass("card");
    }
}
```

## Complete Example pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.mycompany</groupId>
    <artifactId>my-j2html-project</artifactId>
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

        <!-- j2html -->
        <dependency>
            <groupId>com.j2html</groupId>
            <artifactId>j2html</artifactId>
            <version>1.6.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

## Troubleshooting

### Cannot Resolve Dependency

If Maven cannot find the dependency:

1. **Verify installation:**
   ```cmd
   dir C:\Users\robin\.m2\repository\com\example\j2html-preview-annotations\0.1.0-SNAPSHOT
   ```
   You should see JAR files in this directory.

2. **Re-install the annotations:**
   ```cmd
   cd C:\Users\robin\IdeaProjects\j2html-intellij-plugin
   gradlew.bat :annotations:publishToMavenLocal
   ```

3. **Force Maven to update:**
   ```cmd
   mvn clean install -U
   ```

### Outdated Snapshot Version

SNAPSHOT versions are mutable. To get the latest version:

1. Re-publish from the annotations project:
   ```cmd
   gradlew.bat :annotations:publishToMavenLocal
   ```

2. Force update in your project:
   ```cmd
   mvn clean install -U
   ```

### IntelliJ IDEA Not Recognizing Annotations

1. Reload Maven project: Right-click on `pom.xml` → Maven → Reload project
2. Invalidate caches: File → Invalidate Caches → Invalidate and Restart
3. Verify dependency in Maven tool window (View → Tool Windows → Maven)

## Maven Coordinates Reference

```
Group ID:    com.example
Artifact ID: j2html-preview-annotations
Version:     0.1.0-SNAPSHOT
```

## Next Steps

- Check the main documentation: `/docs/agent/LOCAL_MAVEN_INSTALLATION.md`
- View usage examples: `/docs/USAGE_EXAMPLE.md`
- Explore test files: `/test-files/`

## Notes

- Maven automatically uses the local repository (`~/.m2/repository`) without additional configuration
- SNAPSHOT versions indicate work-in-progress and can be updated by re-publishing
- For team collaboration, consider publishing to a shared Maven repository (e.g., Nexus, Artifactory)
