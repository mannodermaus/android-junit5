package de.mannodermaus.gradle.plugins.junit5.internal.usage

import de.mannodermaus.Libraries
import de.mannodermaus.Libraries.JUnit
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer

/**
 * Helper class to locate the [Usage] of a particular library version in the project.
 */
internal class DependencyUsageDetector(private val project: Project) {
    sealed interface Usage {
        val version: SemVer
    }

    data class JUnitUsage(override val version: SemVer, val junit: JUnit) : Usage
    private data class DefaultUsage(override val version: SemVer) : Usage

    fun findJUnit(configurationName: String): JUnitUsage? {
        val version = find(
            configurationName,
            { it.group == "org.junit.jupiter" && it.name == "junit-jupiter-api" },
            { it.group == "org.junit" && it.name == "junit-bom" }
        ) ?: return null

        // If found, identify a particular framework version
        val junit = JUnit.entries
            .firstOrNull { it.majorVersion == version.major.toInt() }
            ?: return null

        return JUnitUsage(version, junit)
    }

    fun findCompose(configurationName: String): Usage? = find(
        configurationName,
        { it.group?.startsWith("androidx.compose") == true }
    )?.let(::DefaultUsage)

    private fun find(configurationName: String, vararg matchers: (Dependency) -> Boolean): SemVer? {
        val configuration = project.configurations.getByName(configurationName)

        val matchingDependency = configuration.dependencies
            .firstOrNull { dependency -> matchers.any { it(dependency) } }

        return matchingDependency?.version?.let { SemVer.from(it) }
    }
}
