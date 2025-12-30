package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.util.projects.PluginSpecProjectCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Common baseline for AGP-based testing. Different subclasses extend this for every type of Android
 * Gradle Plugin supported by JUnit 5.
 */
abstract class AbstractProjectTests(
    private val pluginApplier:
        ((PluginSpecProjectCreator.Builder) -> PluginSpecProjectCreator.Builder)
) :
    AgpConfigurationParameterTests,
    AgpFilterTests,
    AgpInstrumentationSupportTests,
    AgpJacocoBaseTests,
    AgpJacocoExclusionRuleTests,
    AgpJacocoVariantTests,
    AgpVariantTests {

    @RegisterExtension @JvmField val projectExtension = TestProjectProviderExtension()

    override fun createProject(): PluginSpecProjectCreator.Builder {
        return projectExtension.newProject().also { pluginApplier(it) }
    }

    override fun defaultBuildTypes() = listOf("debug", "release")

    override fun defaultProductFlavors() =
        listOf(
            FlavorSpec(name = "free", dimension = "tier"),
            FlavorSpec(name = "paid", dimension = "tier"),
        )

    @Test
    fun `add an extension to testOptions`() {
        val project = createProject().buildAndEvaluate()

        assertThat(project.junitPlatform).isNotNull()
    }
}
