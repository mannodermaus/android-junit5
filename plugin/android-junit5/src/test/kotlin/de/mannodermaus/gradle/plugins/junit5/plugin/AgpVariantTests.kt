package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.capitalized
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.junitPlatform
import org.junit.jupiter.api.TestFactory

interface AgpVariantTests : AgpVariantAwareTests {

    @TestFactory
    fun `does not create a build-type-specific jacoco task`() =
        forEachBuildType { project, buildType ->
            val buildTypeName = buildType.capitalized()
            val name = "jacocoTestReport$buildTypeName"

            assertThat(project.tasks.findByName(name)).isNull()
        }

    @TestFactory
    fun `add build-type-specific filter DSL to the extension`() =
        forEachBuildType { project, buildType ->
            assertThat(project.junitPlatform.filters(buildType)).isNotNull()
        }

    @TestFactory
    fun `add a flavor-specific filter DSL to the extension`() =
        forEachProductFlavor { project, flavor ->
            assertThat(project.junitPlatform.filters(flavor)).isNotNull()
        }

    @TestFactory
    fun `add a variant-specific filter DSL to the extension`() =
        forEachVariant { project, variant ->
            assertThat(project.junitPlatform.filters(variant)).isNotNull()
        }
}
