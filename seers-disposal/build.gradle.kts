plugins {
    id("bundle.mod")
}

dependencies {
    implementation(project(":seers-core"))

    gametestImplementation(testFixtures(project(":seers-core")))
}
