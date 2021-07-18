package de.mannodermaus.gradle.plugins.junit5.plugin

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.FiltersExtension
import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.extensionByName
import de.mannodermaus.gradle.plugins.junit5.junitPlatform
import de.mannodermaus.gradle.plugins.junit5.util.evaluate
import de.mannodermaus.gradle.plugins.junit5.util.get
import de.mannodermaus.gradle.plugins.junit5.util.junitPlatformOptions
import org.gradle.api.Action
import org.gradle.api.tasks.testing.Test
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

interface AgpFilterTests : AgpVariantAwareTests {

    @org.junit.jupiter.api.Test
    fun `add filter DSL to the extension`() {
        val project = createProject().buildAndEvaluate()
        val ju5 = project.junitPlatform
        val filtersExtension = ju5.extensionByName<FiltersExtension>("filters")

        assertThat(filtersExtension).isNotNull()
        assertThat(ju5.filters).isEqualTo(filtersExtension)
        assertThat(ju5.findFilters()).isEqualTo(filtersExtension)
        assertThat(ju5.findFilters(qualifier = null)).isEqualTo(filtersExtension)
    }

    @TestFactory
    fun `apply global filter configuration correctly`() = forEachBuildType(
            beforeEvaluate = { project ->
                project.junitPlatform.filters {
                    includeTags("global-include-tag")
                    excludeTags("global-exclude-tag")
                    includeEngines("global-include-engine")
                    excludeEngines("global-exclude-engine")
                    includePattern("com.example.package1")
                    excludePattern("com.example.package2")
                }
            }
    ) { project, buildType ->
        val task = project.tasks.get<Test>("test${buildType.capitalize()}UnitTest")
        assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag")
        assertThat(task.junitPlatformOptions.excludeTags).contains("global-exclude-tag")
        assertThat(task.junitPlatformOptions.includeEngines).contains("global-include-engine")
        assertThat(task.junitPlatformOptions.excludeEngines).contains("global-exclude-engine")
        assertThat(task.includes).contains("com.example.package1")
        assertThat(task.excludes).contains("com.example.package2")
    }

    @TestFactory
    fun `using custom build types & multiple flavor dimensions`(): List<DynamicTest> {
        val project = createProject().build()
        project.registerProductFlavors(advancedFlavorList)
        project.android.buildTypes { container ->
            container.create("ci").initWith(container.findByName("debug"))
        }
        project.evaluate()

        return advancedFilterDslNames.map { filterName ->
            dynamicTest("add $filterName DSL to the extension") {
                val filtersExtension = project.junitPlatform.extensionByName<FiltersExtension>(filterName)
                assertThat(filtersExtension).isNotNull()
            }
        }
    }

    @TestFactory
    fun `using flavor-specific filters`(): List<DynamicTest> {
        val project = createProject().build()
        project.registerProductFlavors()
        project.junitPlatform {
            filters {
                includeTags("global-include-tag")
                excludeTags("global-exclude-tag")
                includePattern("com.example.package1")
            }
            filters("paid", Action {
                it.includeEngines("paid-include-engine")
                it.includePattern("com.example.paid")
                it.excludePattern("com.example.package1")
            })
            filters("freeDebug", Action {
                it.includeTags("freeDebug-include-tag")
            })
            filters("paidRelease", Action {
                it.includeTags("paidRelease-include-tag")
                it.includeTags("global-exclude-tag")
                it.includePattern("com.example.paid.release")
            })
        }
        project.evaluate()

        return listOf(
                dynamicTest("apply freeDebug filters correctly") {
                    val task = project.tasks.get<Test>("testFreeDebugUnitTest")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .containsAllOf("global-include-tag", "freeDebug-include-tag")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .doesNotContain("paidRelease-include-tag")
                    assertThat(task.junitPlatformOptions.excludeTags)
                            .contains("global-exclude-tag")

                    assertThat(task.junitPlatformOptions.includeEngines)
                            .doesNotContain("paid-include-engine")

                    assertThat(task.includes).contains("com.example.package1")
                    assertThat(task.includes).doesNotContain("com.example.paid")
                    assertThat(task.includes).doesNotContain("com.example.paid.release")
                },

                dynamicTest("apply freeRelease filters correctly") {
                    val task = project.tasks.get<Test>("testFreeReleaseUnitTest")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .contains("global-include-tag")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .doesNotContain("freeDebug-include-tag")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .doesNotContain("paidRelease-include-tag")
                    assertThat(task.junitPlatformOptions.excludeTags)
                            .contains("global-exclude-tag")

                    assertThat(task.junitPlatformOptions.includeEngines)
                            .doesNotContain("paid-include-engine")

                    assertThat(task.includes).contains("com.example.package1")
                    assertThat(task.includes).doesNotContain("com.example.paid")
                    assertThat(task.includes).doesNotContain("com.example.paid.release")
                },

                dynamicTest("apply paidDebug filters correctly") {
                    val task = project.tasks.get<Test>("testPaidDebugUnitTest")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .contains("global-include-tag")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .doesNotContain(
                                    "freeDebug-include-tag")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .doesNotContain(
                                    "paidRelease-include-tag")
                    assertThat(task.junitPlatformOptions.excludeTags)
                            .contains("global-exclude-tag")

                    assertThat(task.junitPlatformOptions.includeEngines)
                            .contains("paid-include-engine")

                    assertThat(task.includes).contains("com.example.paid")
                    assertThat(task.excludes).contains("com.example.package1")
                    assertThat(task.includes).doesNotContain("com.example.package1")
                    assertThat(task.includes).doesNotContain("com.example.paid.release")
                },

                dynamicTest("apply paidRelease filters correctly") {
                    val task = project.tasks.get<Test>("testPaidReleaseUnitTest")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .containsAllOf("global-include-tag",
                                    "global-exclude-tag",
                                    "paidRelease-include-tag")
                    assertThat(task.junitPlatformOptions.includeTags)
                            .doesNotContain("freeDebug-include-tag")
                    assertThat(task.junitPlatformOptions.excludeTags)
                            .doesNotContain("global-exclude-tag")

                    assertThat(task.junitPlatformOptions.includeEngines)
                            .contains("paid-include-engine")

                    assertThat(task.includes)
                            .containsAllOf("com.example.paid", "com.example.paid.release")
                    assertThat(task.includes).doesNotContain("com.example.package1")
                    assertThat(task.excludes).contains("com.example.package1")
                }
        )
    }

    @TestFactory
    fun `using build-type-specific filters`(): List<DynamicTest> {
        val project = createProject().build()
        project.junitPlatform {
            filters {
                includeTags("global-include-tag")
                includeEngines("global-include-engine")
                includePattern("pattern123")
            }
            filters("debug", Action {
                it.excludeTags("debug-exclude-tag")
                it.excludeEngines("debug-exclude-engine")
                it.excludePattern("pattern123")
                it.excludePattern("debug-pattern")
            })
            filters("release", Action {
                it.includeTags("rel-include-tag")
                it.includeEngines("rel-include-engine")
                it.excludeEngines("global-include-engine")
                it.includePattern("release-pattern")
            })
        }
        project.evaluate()

        return listOf(
                dynamicTest("apply debug filters correctly") {
                    val task = project.tasks.get<Test>("testDebugUnitTest")
                    assertThat(task.junitPlatformOptions.includeTags).contains("global-include-tag")
                    assertThat(task.junitPlatformOptions.includeTags).doesNotContain("rel-include-tag")
                    assertThat(task.junitPlatformOptions.excludeTags).contains("debug-exclude-tag")

                    assertThat(task.junitPlatformOptions.includeEngines).contains("global-include-engine")
                    assertThat(task.junitPlatformOptions.includeEngines).doesNotContain(
                            "rel-include-engine")
                    assertThat(task.junitPlatformOptions.excludeEngines).contains("debug-exclude-engine")

                    assertThat(task.includes).doesNotContain("pattern123")
                    assertThat(task.excludes).containsAllOf("pattern123", "debug-pattern")
                },

                dynamicTest("apply release filters correctly") {
                    val task = project.tasks.get<Test>("testReleaseUnitTest")
                    assertThat(task.junitPlatformOptions.includeTags).containsAllOf("global-include-tag",
                            "rel-include-tag")
                    assertThat(task.junitPlatformOptions.excludeTags).doesNotContain("debug-exclude-tag")

                    assertThat(task.junitPlatformOptions.includeEngines).contains("rel-include-engine")
                    assertThat(task.junitPlatformOptions.includeEngines).doesNotContain(
                            "global-include-engine")
                    assertThat(task.junitPlatformOptions.excludeEngines).contains("global-include-engine")
                    assertThat(task.junitPlatformOptions.excludeEngines).doesNotContain(
                            "debug-exclude-engine")

                    assertThat(task.includes).containsAllOf("pattern123", "release-pattern")
                    assertThat(task.excludes).doesNotContain("pattern123")
                }
        )
    }
}

private val advancedFlavorList =
        listOf(
                FlavorSpec(name = "brandA", dimension = "brand"),
                FlavorSpec(name = "brandB", dimension = "brand"),
                FlavorSpec(name = "development", dimension = "environment"),
                FlavorSpec(name = "production", dimension = "environment"),
                FlavorSpec(name = "free", dimension = "payment"),
                FlavorSpec(name = "paid", dimension = "payment")
        )

private val advancedFilterDslNames =
        listOf(
                "filters",

                "debugFilters",
                "releaseFilters",
                "ciFilters",

                "brandAFilters",
                "brandBFilters",

                "developmentFilters",
                "productionFilters",

                "freeFilters",
                "paidFilters",

                "brandADevelopmentPaidDebugFilters",
                "brandADevelopmentPaidReleaseFilters",
                "brandADevelopmentPaidCiFilters",
                "brandADevelopmentFreeDebugFilters",
                "brandADevelopmentFreeReleaseFilters",
                "brandADevelopmentFreeCiFilters",
                "brandAProductionPaidDebugFilters",
                "brandAProductionPaidReleaseFilters",
                "brandAProductionPaidCiFilters",
                "brandAProductionFreeDebugFilters",
                "brandAProductionFreeReleaseFilters",
                "brandAProductionFreeCiFilters",

                "brandBDevelopmentPaidDebugFilters",
                "brandBDevelopmentPaidReleaseFilters",
                "brandBDevelopmentPaidCiFilters",
                "brandBDevelopmentFreeDebugFilters",
                "brandBDevelopmentFreeReleaseFilters",
                "brandBDevelopmentFreeCiFilters",
                "brandBProductionPaidDebugFilters",
                "brandBProductionPaidReleaseFilters",
                "brandBProductionPaidCiFilters",
                "brandBProductionFreeDebugFilters",
                "brandBProductionFreeReleaseFilters",
                "brandBProductionFreeCiFilters"
        )
