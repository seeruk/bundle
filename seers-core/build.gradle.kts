plugins {
    id("bundle.mod")
    `java-test-fixtures`
}

// Test fixtures are helpers for other mods' tests, they shouldn't be published alongside the mod.
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
