package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import org.gradle.api.ProjectConfigurationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension

class WrongPluginUsageTests {

    @RegisterExtension
    @JvmField
    val projectExtension = TestProjectProviderExtension()

    @Test
    fun `not applying any supported Android plugin`() {
        val exception = assertThrows<ProjectConfigurationException> {
            projectExtension.newProject().buildAndEvaluate()
        }
        assertThat(exception.cause?.message)
            .contains("An Android plugin must be applied in order for android-junit5 to work correctly!")
    }
}
