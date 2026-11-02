package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * A task that copies the seers-template mod as a new mod, renaming everything based on the given
 * display name; for example, "Seer's PropSwap" becomes seers-propswap, SeersPropSwap,
 * dev.seeruk.bundle.propswap, etc.
 */
abstract class ScaffoldMod : DefaultTask() {
    @get:Input
    @get:Option(option = "name", description = "Display name of the new mod, e.g. \"Seer's PropSwap\"")
    abstract val modName: Property<String>

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    init {
        doNotTrackState("Helper task for copying a template mod. Not part of the build")
    }

    @TaskAction
    fun createMod() {
        val name = modName.get().trim()
        if (!name.matches(Regex("""[ -~&&[^"\\]]+"""))) { // Disallow " and \
            throw GradleException("The display name must only contain ASCII characters, got: $name")
        }

        val words = Regex("[A-Za-z0-9]+").findAll(name.replace("'", "")).map { it.value }.toList()
        if (words.isEmpty()) {
            throw GradleException("The display name must contain at least one letter or number, got: $name")
        }
        if (words.first().equals("seers", ignoreCase = true)) {
            throw GradleException("The display name must not start with \"Seer's\", got: $name")
        }

        val modId = words.joinToString("-").lowercase()
        val classPrefix = words.joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }

        val packageName = words.joinToString("").lowercase()
        if (packageName.isEmpty() || !packageName.first().isLetter()) {
            throw GradleException("The resulting package name is invalid: '$packageName'")
        }

        val root = rootDirectory.get().asFile
        val template = root.resolve("seers-template")
        val target = root.resolve("seers-$modId")
        if (target.exists()) {
            throw GradleException("A folder already exists at this location: $target")
        }

        val replacements = mapOf(
            "Seer's Template" to "Seer's $name",
            "SeersTemplate" to "Seers$classPrefix",
            "seers-template" to "seers-$modId",
            "dev.seeruk.bundle.template" to "dev.seeruk.bundle.$packageName",
            "dev/seeruk/bundle/template" to "dev/seeruk/bundle/$packageName",
        )

        fun String.rewrite() =
            replacements.entries.fold(this) { acc, (from, to) -> acc.replace(from, to) }

        val skipped = setOf("build", "run", ".gradle")
        val textExtensions = setOf("java", "json", "kts", "properties", "md")

        template.walkTopDown()
            .onEnter { it == template || it.relativeTo(template).path !in skipped }
            .filter { it.isFile }
            .forEach { source ->
                val relPath = source.relativeTo(template).invariantSeparatorsPath
                val out = target.resolve(relPath.rewrite())

                out.parentFile.mkdirs()

                if (source.extension in textExtensions) {
                    out.writeText(source.readText().rewrite())
                } else {
                    source.copyTo(out)
                }
            }

        // Create a new README for the mod, to be filled in.
        target.resolve("README.md").writeText("# Seer's $name\n\nTODO\n")

        // Append the new module to the bundle.modules list so it's included in the build
        val propsFile = root.resolve("gradle.properties")
        val propsText = propsFile.readText()

        val modulesPattern = Regex("""(?m)^bundle\.modules=(?:.*\\\r?\n)*.*$""")
        val modulesMatch = modulesPattern.find(propsText)
            ?: throw GradleException("Couldn't find bundle.modules in $propsFile")

        val modules = modulesMatch.value
            .substringAfter("=")
            .replace("\\\r\n", "")
            .replace("\\\n", "")
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .plus("seers-$modId")
            .distinct()
            .sorted()

        val updated = modules.joinToString(",\\\n  ", prefix = "bundle.modules=\\\n  ")

        propsFile.writeText(
            propsText.replaceRange(modulesMatch.range, updated)
        )

        logger.lifecycle("$name created under seers-$modId/")
    }
}
