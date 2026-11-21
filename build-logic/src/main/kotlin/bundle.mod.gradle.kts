import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `java-library`
    `maven-publish`

    id("net.fabricmc.fabric-loom")
}

val libs = the<LibrariesForLibs>()
val javaVersion = libs.versions.java.get().toInt()

group = "dev.seeruk.bundle"

repositories {
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    testImplementation(libs.fabric.loader.junit)
    testImplementation(libs.junit.jupiter.params)
}

// Mods with a src/client directory get an optional client part: client-only code in its own source
// set, so it's never loaded on a dedicated server. It ships in the same jar as the rest of the mod.
val hasClient = file("src/client").exists()

loom {
    if (hasClient) {
        splitEnvironmentSourceSets()
    }

    mods {
        register(project.name) {
            sourceSet(sourceSets.main.get())
            if (hasClient) {
                sourceSet(sourceSets.getByName("client"))
            }
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    val junitDir = layout.buildDirectory.dir("junit")
    doFirst { junitDir.get().asFile.mkdirs() }

    workingDir(junitDir)
}

val hasGameTests = file("src/gametest").exists()
val hasClientGameTests =
    file("src/gametest/java").walkTopDown()
        .any { it.isFile && it.name.endsWith("ClientGameTest.java") }

// Set up every mod with a GameTest directory to use a conventional GameTest setup.
if (hasGameTests) {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = "${project.name}-gametest"
            enableGameTests = true
            enableClientGameTests = hasClientGameTests
        }
    }

    // Each run starts with a fresh world, so nothing a test stores (e.g. nicknames) is left over
    // from earlier runs to clash with it. Loom's clearRunDirectory option doesn't do this for
    // server GameTests, so it's done here.
    val deleteGameTestWorld = tasks.register<Delete>("deleteGameTestWorld") {
        delete(layout.buildDirectory.dir("run/gameTest/world"))
    }
    tasks.named { it == "runGameTest" }.configureEach {
        dependsOn(deleteGameTestWorld)
    }
}

// Set every mod to compile against the right Java version.
tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion
}

// Set the {version} tag to the actual built mod version.
tasks.withType<ProcessResources>().configureEach {
    val version = project.version.toString()
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

// Inject our licence into the built jars.
tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)

    from(rootProject.file("LICENSE")) {
        rename { "${it}_$projectName" }
    }
}

// Tag every mod jar produced in bundle with an attribute to identify it as such.
// This allows us to easily extract all the mod jars later.
configurations.consumable("bundleJar") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("bundle-mod-jar"))
    }
    outgoing.artifact(tasks.jar)
}

// Ignore the template mod.
if (project.name != "seers-template") {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["java"])
            }
        }
    }
}
