plugins {
    id("bundle.mod")
}

dependencies {
    implementation(project(":seers-core"))
    implementation(project(":seers-text"))

    testImplementation(testFixtures(project(":seers-core")))
    gametestImplementation(testFixtures(project(":seers-core")))
}
