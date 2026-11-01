plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    // Allows the use of `libs` in these precompiled script plugins.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}