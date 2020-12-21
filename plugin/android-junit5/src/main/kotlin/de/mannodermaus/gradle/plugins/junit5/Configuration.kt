package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import de.mannodermaus.gradle.plugins.junit5.Type.*
import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.hasPlugin
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

/**
 * Utility class, used for controlled access
 * to a Project's configuration.
 *
 * This class provides a safe interface to access the
 * properties specific to the Android Gradle Plugin
 * in a backwards-compatible manner. It will raise a
 * [ProjectConfigurationException] early, whenever the plugin
 * is not applied in an Android environment.
 */
class ProjectConfig(val project: Project) {
  private val type: Type<BaseExtension> = findType(project)

  val variants get() = type.variants(project.android)
  val jacocoPluginApplied get() = project.hasPlugin("jacoco")
  val kotlinPluginApplied get() = project.hasPlugin("kotlin-android")
}

private sealed class Type<in T : BaseExtension>(val pluginId: String) {
  abstract fun variants(extension: T): DomainObjectSet<out BaseVariant>

  object Application : Type<AppExtension>("com.android.application") {
    override fun variants(extension: AppExtension): DomainObjectSet<ApplicationVariant> =
        extension.applicationVariants
  }

  object Library : Type<LibraryExtension>("com.android.library") {
    override fun variants(extension: LibraryExtension): DomainObjectSet<LibraryVariant> {
      return extension.libraryVariants
    }
  }

  // Although there are "featureVariants" for modules applying the Feature plugin,
  // there is no distinct per-feature test task per se.
  // Therefore, we use the default library variants here
  object Feature : Type<FeatureExtension>("com.android.feature") {
    override fun variants(extension: FeatureExtension): DomainObjectSet<LibraryVariant> =
        extension.libraryVariants
  }

  object DynamicFeature : Type<AppExtension>("com.android.dynamic-feature") {
    override fun variants(extension: AppExtension): DomainObjectSet<ApplicationVariant> =
        extension.applicationVariants
  }
}

private val allTypes: List<Type<*>> =
    listOf(Application, Library, Feature, DynamicFeature)

@Suppress("UNCHECKED_CAST")
private fun findType(project: Project): Type<BaseExtension> {
  val type = allTypes.firstOrNull {
    project.plugins.hasPlugin(it.pluginId)
  }

  if (type == null) {
    val supportedPluginNames = allTypes.map { it.pluginId }
    throw ProjectConfigurationException("One of the following plugins must be applied to this project: $supportedPluginNames", IllegalArgumentException())
  } else {
    return type as Type<BaseExtension>
  }
}

internal class JUnit5TaskConfig(
    private val variant: BaseVariant,
    project: Project) {

  private val extension = project.junitPlatform

  // There is a distinct application order, which determines how values are merged and overwritten.
  // From top to bottom, this list goes as follows (values on the bottom will override conflicting
  // entries specified above them):
  // 1) Default ("filters")
  // 2) Build-type-specific (e.g. "debug")
  // 3) Flavor-specific (e.g. "free")
  // 4) Variant-specific (e.g. "freeDebug")
  private fun collect(
      func: FiltersExtension.() -> IncludeExcludeContainer): IncludeExcludeContainer {
    // 1)
    val layer1 = filtersOf(null, func)
    // 2)
    val layer2 = layer1 + filtersOf(variant.buildType.name, func)
    // 3)
    val layer3 = variant.productFlavors
        .map { filtersOf(it.name, func) }
        .fold(layer2) { a, b -> a + b }
    // 4)
    return layer3 + filtersOf(variant.name, func)
  }

  private inline fun filtersOf(
      qualifier: String?,
      func: FiltersExtension.() -> IncludeExcludeContainer) =
      if (qualifier == null) {
        extension.filters.func()
      } else {
        extension.findFilters(qualifier).func()
      }

  val combinedIncludePatterns = this.collect { patterns }.include.toTypedArray()
  val combinedExcludePatterns = this.collect { patterns }.exclude.toTypedArray()
  val combinedIncludeTags = this.collect { tags }.include.toTypedArray()
  val combinedExcludeTags = this.collect { tags }.exclude.toTypedArray()
  val combinedIncludeEngines = this.collect { engines }.include.toTypedArray()
  val combinedExcludeEngines = this.collect { engines }.exclude.toTypedArray()
}
