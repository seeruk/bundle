import tasks.ScaffoldMod

// Adds a task for creating a new mod from the template, e.g.:
//   ./gradlew scaffoldMod --name="Seer's PropSwap"
tasks.register<ScaffoldMod>("scaffoldMod") {
    group = "bundle"
    description = "Scaffolds a new mod by copying the seers-template mod"
    rootDirectory = layout.projectDirectory
}
