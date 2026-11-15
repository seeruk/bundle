rootProject.name = "bundle"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Modules specified in the `bundle.modules` property are included for build, allowing projects to
// be disabled while working on updates, etc.
val modules = providers.gradleProperty("bundle.modules").get()
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

include(modules)

// Runs every included mod together, but isn't a mod itself
include("bundle-all")
