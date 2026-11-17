plugins {
    id("bundle.mod")
}

dependencies {
    api(libs.placeholder.api)

    testImplementation(testFixtures(project(":seers-core")))
}
