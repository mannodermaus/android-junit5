package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

interface AgpConfigurationParameterTests : AgpTests {
    @Test
    fun `throw exception if configuration parameter key is empty`() {
        val project = createProject().build()

        val exception =
            assertThrows<IllegalArgumentException> {
                project.junitPlatform.configurationParameter("", "some-value")
            }
        assertThat(exception.message).contains("key must not be blank")
    }

    @Test
    fun `throw exception if configuration parameter key contains illegal characters`() {
        val project = createProject().build()

        val exception =
            assertThrows<IllegalArgumentException> {
                project.junitPlatform.configurationParameter("illegal=key", "some-value")
            }
        assertThat(exception.message).contains("key must not contain '='")
    }
}
