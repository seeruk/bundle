plugins {
    id("bundle.mod")
}

// We create a new dependency scope to hold our top-level non-mod dependencies. Loom only bundles
// the libraries it's given, not their dependencies. With this approach each library they resolve
// can also be bundled.
val bundled = configurations.dependencyScope("bundled")

// Every library the bundled dependencies resolve to
val bundledLibraries = configurations.resolvable("bundledLibraries") {
    extendsFrom(bundled.get())
}

configurations.implementation {
    extendsFrom(bundled.get())
}

// Tell Loom to include all of the libraries we've resolved for "bundled" dependencies.
configurations.include {
    dependencies.addAllLater(
        bundledLibraries
            .flatMap { it.incoming.artifacts.resolvedArtifacts }
            .map { artifacts ->
                artifacts.map { project.dependencies.create(it.id.componentIdentifier.displayName) }
            })
}

dependencies {
    // Excluded dependencies are either not required, or already included by Minecraft

    bundled(libs.jda) {
        // JDA usually includes support for voice, which we don't need. SLF4J is also already
        // included in Minecraft already, so we don't need that.
        exclude(group = "club.minnced", module = "opus-java")
        exclude(group = "com.google.crypto.tink", module = "tink")
        exclude(group = "org.slf4j")
    }

    bundled(libs.jemoji) {
        // JSpecify is already pulled in by Minecraft.
        exclude(group = "org.jspecify")
    }

    bundled(libs.simpleast)

    implementation(project(":seers-core"))
    implementation(project(":seers-text"))

    testImplementation(testFixtures(project(":seers-core")))
    gametestImplementation(testFixtures(project(":seers-core")))
}
