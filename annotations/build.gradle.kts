plugins {
    id("java")
    id("maven-publish")
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    
    // Create sources jar for publishing
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    
    withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            artifactId = "j2html-preview-annotations"
            
            pom {
                name.set("j2html Preview Annotations")
                description.set("Annotations for marking j2html component preview methods")
                url.set("https://github.com/teggr/j2html-intellij-plugin")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("teggr")
                        name.set("teggr")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/teggr/j2html-intellij-plugin.git")
                    developerConnection.set("scm:git:ssh://github.com/teggr/j2html-intellij-plugin.git")
                    url.set("https://github.com/teggr/j2html-intellij-plugin")
                }
            }
        }
    }
}
