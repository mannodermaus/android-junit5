@file:Suppress("UnstableApiUsage")

package de.mannodermaus.gradle.plugins.junit5.internal.config

import com.android.build.api.variant.Variant
import de.mannodermaus.gradle.plugins.junit5.dsl.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.junit5.dsl.FiltersExtension
import de.mannodermaus.gradle.plugins.junit5.internal.utils.IncludeExcludeContainer

internal class JUnitPlatformTaskConfig(
    private val variant: Variant,
    private val extension: AndroidJUnitPlatformExtension,
) {

    // There is a distinct application order, which determines how values are merged and
    // overwritten.
    // From top to bottom, this list goes as follows (values on the bottom will override conflicting
    // entries specified above them):
    // 1) Default ("filters")
    // 2) Build-type-specific (e.g. "debug")
    // 3) Flavor-specific (e.g. "free")
    // 4) Variant-specific (e.g. "freeDebug")
    private fun collect(
        func: FiltersExtension.() -> IncludeExcludeContainer
    ): IncludeExcludeContainer {
        // 1)
        val layer1 = filtersOf(null, func)
        // 2)
        val layer2 = layer1 + filtersOf(variant.buildType, func)
        // 3)
        val layer3 =
            variant.productFlavors.map { filtersOf(it.second, func) }.fold(layer2) { a, b -> a + b }
        // 4)
        return layer3 + filtersOf(variant.name, func)
    }

    private inline fun filtersOf(
        qualifier: String?,
        func: FiltersExtension.() -> IncludeExcludeContainer,
    ): IncludeExcludeContainer = extension.filters(qualifier).run { func() }

    val combinedIncludePatterns = this.collect { patterns }.include.toTypedArray()
    val combinedExcludePatterns = this.collect { patterns }.exclude.toTypedArray()
    val combinedIncludeTags = this.collect { tags }.include.toTypedArray()
    val combinedExcludeTags = this.collect { tags }.exclude.toTypedArray()
    val combinedIncludeEngines = this.collect { engines }.include.toTypedArray()
    val combinedExcludeEngines = this.collect { engines }.exclude.toTypedArray()
}
