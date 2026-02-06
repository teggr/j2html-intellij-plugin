// Root project configuration
// This is now a multi-module project with:
// - annotations: A standalone Java library for the @Preview annotation
// - plugin: The IntelliJ IDEA plugin that depends on annotations

plugins {
    id("org.jetbrains.intellij") version "1.17.4" apply false
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

// Common configuration for all subprojects
subprojects {
    repositories {
        mavenCentral()
    }
}
