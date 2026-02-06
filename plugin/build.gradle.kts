plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
}

dependencies {
    // Depend on the annotations module
    implementation(project(":annotations"))
    
    // j2html library
    implementation("com.j2html:j2html:1.6.0")
}

// Configure the IntelliJ Platform plugin
intellij {
    version.set("2024.1")
    type.set("IC")  // IC = Community Edition, IU = Ultimate
    plugins.set(listOf("java"))
    downloadSources.set(false)
}

tasks {
    // Set Java compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
    }
}
