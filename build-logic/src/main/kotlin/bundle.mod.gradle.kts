import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `java-library`
    `maven-publish`
}

val libs = the<LibrariesForLibs>()
val javaVersion = libs.versions.java.get().toInt()

group = "dev.seeruk.bundle"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
