// Runs the game with every mod in the bundle at once (e.g. `./gradlew :bundle-all:runClient`), to
// see how they work together. It's not a mod itself, so it's never built into a jar or published.
plugins {
    id("net.fabricmc.fabric-loom")
}

repositories {
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
}

val bundleMods = rootProject.subprojects.filter { it.name.startsWith("seers-") && it.name != "seers-template" }

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    bundleMods.forEach { implementation(project(it.path)) }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
}

loom {
    mods {
        bundleMods.forEach { mod ->
            register(mod.name) {
                sourceSet("main", mod.path)
                if (mod.file("src/client").exists()) {
                    sourceSet("client", mod.path)
                }
            }
        }
    }
}
