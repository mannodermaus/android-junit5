package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.FiltersExtension
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.extensionByName
import de.mannodermaus.gradle.plugins.junit5.junitPlatform
import org.junit.jupiter.api.TestFactory

interface AgpVariantTests : AgpVariantAwareTests {

    @TestFactory
    fun `does not create a build-type-specific jacoco task`() = forEachBuildType { project, buildType ->
        val buildTypeName = buildType.capitalize()
        val name = "jacocoTestReport$buildTypeName"

        assertThat(project.tasks.findByName(name)).isNull()
    }

    @TestFactory
    fun `add build-type-specific filter DSL to the extension`() = forEachBuildType { project, buildType ->
        val name = "${buildType}Filters"
        val extension = project.junitPlatform.extensionByName<FiltersExtension>(name)

        assertThat(extension).isNotNull()
        assertThat(project.junitPlatform.findFilters(qualifier = buildType)).isEqualTo(extension)
    }

    @TestFactory
    fun `add a flavor-specific filter DSL to the extension`() = forEachProductFlavor { project, flavor ->
        val name = "${flavor}Filters"
        val extension = project.junitPlatform.extensionByName<FiltersExtension>(name)

        assertThat(extension).isNotNull()
        assertThat(project.junitPlatform.findFilters(qualifier = flavor)).isEqualTo(extension)
    }

    @TestFactory
    fun `add a variant-specific filter DSL to the extension`() = forEachVariant { project, variant ->
        val name = "${variant}Filters"
        val extension = project.junitPlatform.extensionByName<FiltersExtension>(name)

        assertThat(extension).isNotNull()
        assertThat(project.junitPlatform.findFilters(qualifier = variant)).isEqualTo(extension)
    }
}
