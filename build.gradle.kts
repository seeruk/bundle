import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    base

    id("com.diffplug.spotless") version "8.10.4"
}

val bundleJars = configurations.dependencyScope("bundleJars")
val bundleJarFiles = configurations.resolvable("bundleJarFiles") {
    extendsFrom(bundleJars.get())
    isTransitive = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("bundle-mod-jar"))
    }
}

dependencies {
    subprojects.forEach { bundleJars(project(it.path)) }
}

subprojects {
    // Set up code formatting:
    pluginManager.withPlugin("java") {
        pluginManager.apply("com.diffplug.spotless")

        extensions.configure<SpotlessExtension> {
            java {
                palantirJavaFormat("2.103.0")
                    .formatJavadoc(true)
                removeUnusedImports()
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }
}

val collectJars by tasks.registering(Sync::class) {
    group = "build"
    description = "Collects every mod jar into the root build/libs directory."
    from(bundleJarFiles)
    into(layout.buildDirectory.dir("libs"))
}

tasks.assemble {
    dependsOn(collectJars)
}
