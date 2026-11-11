plugins {
    id("bundle.mod")
    `java-test-fixtures`
}

// Test fixtures are helpers for other mods' tests, they shouldn't be published alongside the mod.
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

dependencies {
    // Test fixtures can use Minecraft classes, consumers' test classpaths already have them at runtime.
    testFixturesCompileOnly(files(sourceSets.main.map { it.compileClasspath }))

    // Core's own GameTests use the test fixtures too.
    gametestImplementation(testFixtures(project))
}
