plugins {
    base

    id("bundle.tasks")
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
    subprojects
        // Ignore the template mod.
        .filter { it.name != "seers-template" }
        .forEach { bundleJars(project(it.path)) }
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
