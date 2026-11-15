plugins {
    id("bundle.mod")
}

// JDA and everything it needs is bundled in the mod's jar, except voice support (which a chat bridge
// never uses), and SLF4J (which the game already has). Loom only bundles the libraries it's given,
// not their own dependencies, so each library JDA resolves to is bundled. Fabric loads the newest
// version of each library any mod bundles, so ones other mods also bundle (e.g. Jackson) aren't
// loaded twice.
val bundled: Configuration by configurations.creating

configurations.implementation {
    extendsFrom(bundled)
}

configurations.include {
    dependencies.addAllLater(bundled.incoming.artifacts.resolvedArtifacts.map { artifacts ->
        artifacts.map { project.dependencies.create(it.id.componentIdentifier.displayName) }
    })
}

dependencies {
    bundled(libs.jda) {
        exclude(group = "club.minnced", module = "opus-java")
        exclude(group = "com.google.crypto.tink", module = "tink")
        exclude(group = "org.slf4j")
    }

    implementation(project(":seers-core"))
    implementation(project(":seers-text"))

    gametestImplementation(testFixtures(project(":seers-core")))
}
