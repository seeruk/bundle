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
        // Only mods, and not the template mod.
        .filter { it.name.startsWith("seers-") && it.name != "seers-template" }
        .forEach { bundleJars(dependencies.project(it.path)) }
}

val collectJars = tasks.register<Sync>("collectJars") {
    group = "build"
    description = "Collects every mod jar into the root build/libs directory."
    from(bundleJarFiles)
    into(layout.buildDirectory.dir("libs"))
}

tasks.assemble {
    dependsOn(collectJars)
}
