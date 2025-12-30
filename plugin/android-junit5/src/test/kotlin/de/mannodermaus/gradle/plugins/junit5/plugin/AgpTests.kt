package de.mannodermaus.gradle.plugins.junit5.plugin

import de.mannodermaus.gradle.plugins.junit5.extensions.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensions.capitalized
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import de.mannodermaus.gradle.plugins.junit5.util.projects.PluginSpecProjectCreator
import de.mannodermaus.gradle.plugins.junit5.util.times
import org.gradle.api.Project
import org.junit.jupiter.api.DynamicTest

interface AgpTests {
    fun createProject(): PluginSpecProjectCreator.Builder
}

data class FlavorSpec(val name: String, val dimension: String)

interface AgpVariantAwareTests : AgpTests {
    fun defaultBuildTypes(): List<String>

    fun defaultProductFlavors(): List<FlavorSpec>

    fun forEachBuildType(
        beforeBuild: ((PluginSpecProjectCreator.Builder) -> Unit) = {},
        beforeEvaluate: (Project) -> Unit = {},
        testBody: (Project, String) -> Unit,
    ): List<DynamicTest> {
        val parentTestName = parentTestName()

        val project = createProject().also(beforeBuild::invoke).build()
        beforeEvaluate.invoke(project)
        project.evaluate()

        return defaultBuildTypes().map { buildType ->
            DynamicTest.dynamicTest("$parentTestName for '$buildType'") {
                testBody(project, buildType)
            }
        }
    }

    fun forEachProductFlavor(
        flavorCreator: () -> List<FlavorSpec> = { defaultProductFlavors() },
        beforeBuild: ((PluginSpecProjectCreator.Builder) -> Unit) = {},
        beforeEvaluate: (Project) -> Unit = {},
        testBody: (Project, String) -> Unit,
    ): List<DynamicTest> {
        val parentTestName = parentTestName()
        val flavors = flavorCreator()

        val project = createProject().also(beforeBuild::invoke).build()
        project.registerProductFlavors(flavors)
        beforeEvaluate.invoke(project)
        project.evaluate()

        return flavors.map { flavor ->
            DynamicTest.dynamicTest("$parentTestName for '$flavor'") {
                testBody(project, flavor.name)
            }
        }
    }

    fun forEachVariant(
        flavorCreator: () -> List<FlavorSpec> = { defaultProductFlavors() },
        beforeBuild: ((PluginSpecProjectCreator.Builder) -> Unit) = {},
        beforeEvaluate: (Project) -> Unit = {},
        testBody: (Project, String) -> Unit,
    ): List<DynamicTest> {
        val parentTestName = parentTestName()
        val flavors = flavorCreator()

        val project = createProject().also(beforeBuild::invoke).build()
        project.registerProductFlavors(flavors)
        beforeEvaluate.invoke(project)
        project.evaluate()

        return (defaultBuildTypes() * flavors)
            .map { (buildType, flavor) ->
                val variantName = "${flavor.name}${buildType.capitalized()}"

                DynamicTest.dynamicTest("$parentTestName for '$variantName'") {
                    testBody(project, variantName)
                }
            }
            .toList()
    }

    // Helpers and extensions

    private fun parentTestName(): String {
        // Try fetching name of parent test and use that for the child test as well
        return Exception().stackTrace.getOrNull(5)?.methodName ?: "unknown test"
    }

    fun Project.registerProductFlavors(flavors: List<FlavorSpec> = defaultProductFlavors()) {
        val dimensions = flavors.map(FlavorSpec::dimension).distinct()

        with(project.android) {
            flavorDimensions.addAll(dimensions)
            flavors.forEach { flavor ->
                productFlavors.create(flavor.name).dimension = flavor.dimension
            }
        }
    }
}
