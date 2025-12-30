package de.mannodermaus.gradle.plugins.junit5.internal.usage

import de.mannodermaus.Libraries.JUnit
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/** Helper class to locate the usage of a particular library version in the project. */
internal class DependencyUsageDetector(private val project: Project) {
    data class JUnitUsage(val junit: JUnit)

    fun isUsingJUnit(configurationName: String): JUnitUsage? {
        val match =
            find(
                configurationName,
                { it.group == "org.junit" && it.name == "junit-bom" },
                { it.group == "org.junit.jupiter" && it.name == "junit-jupiter-api" },
            ) ?: return null

        // If found, identify a particular framework version.
        // For non-numeric versions (i.e. '+'), use the latest version instead
        val majorVersion = match.version?.substringBefore('.')?.toIntOrNull()
        val junit =
            JUnit.entries.firstOrNull { it.majorVersion == majorVersion } ?: JUnit.entries.last()

        return JUnitUsage(junit)
    }

    fun isUsingCompose(configurationName: String): Boolean {
        return find(configurationName, { it.group?.startsWith("androidx.compose") == true }) != null
    }

    private fun find(
        configurationName: String,
        vararg matchers: (Dependency) -> Boolean,
    ): Dependency? {
        val configuration = project.configurations.getByName(configurationName)

        return configuration.dependencies.firstOrNull { dependency ->
            matchers.any { it(dependency) }
        }
    }
}
