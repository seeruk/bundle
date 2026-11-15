plugins {
    id("bundle.mod")
    `java-test-fixtures`
}

// Test fixtures are helpers for other mods' tests, they shouldn't be published alongside the mod.
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

dependencies {
    // YAML support for config. SnakeYAML is a pretty stable library, so as long as the latest
    // version is used, we should be fine if another mod loads it.
    implementation(libs.snakeyaml.engine)
    include(libs.snakeyaml.engine)

    // Test fixtures can use Minecraft classes, as consumers already have them at runtime.
    testFixturesCompileOnly(files(sourceSets.main.map { it.compileClasspath }))

    // Core's own GameTests use the test fixtures too.
    gametestImplementation(testFixtures(project(path)))
}
