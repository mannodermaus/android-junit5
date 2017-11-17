package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.Type.Application
import de.mannodermaus.gradle.plugins.junit5.Type.Feature
import de.mannodermaus.gradle.plugins.junit5.Type.Library
import de.mannodermaus.gradle.plugins.junit5.Type.Test
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

/* Types */

private sealed class Type<in T : BaseExtension>(val pluginId: String) {
  abstract fun variants(extension: T): Set<BaseVariant>

  object Application : Type<AppExtension>("com.android.application") {
    override fun variants(extension: AppExtension): Set<BaseVariant> =
        extension.applicationVariants
  }

  object Test : Type<TestExtension>("com.android.test") {
    override fun variants(extension: TestExtension): Set<BaseVariant> =
        extension.applicationVariants
  }

  object Library : Type<LibraryExtension>("com.android.library") {
    override fun variants(extension: LibraryExtension): Set<BaseVariant> =
        extension.libraryVariants
  }

  // Although there are "featureVariants" for modules applying the Feature plugin,
  // there is no distinct per-feature test task per se.
  // Therefore, we use the default library variants here
  object Feature : Type<FeatureExtension>("com.android.feature") {
    override fun variants(extension: FeatureExtension): Set<BaseVariant> =
        extension.libraryVariants
  }
}

private val allTypes: List<Type<*>> =
    listOf(Application, Test, Library, Feature)

@Suppress("UNCHECKED_CAST")
private fun findType(project: Project): Type<BaseExtension> {
  val type = allTypes.firstOrNull {
    project.plugins.hasPlugin(it.pluginId)
  }

  if (type == null) {
    throw ProjectConfigurationException("An Android plugin must be applied to this project", null)
  } else {
    return type as Type<BaseExtension>
  }
}

class ProjectConfig(private val project: Project) {
  private val type: Type<BaseExtension> = findType(project)

  val unitTestVariants get() = type.variants(project.android)
  val jacocoPluginApplied get() = project.hasPlugin("jacoco")
  val kotlinPluginApplied get() = project.hasPlugin("kotlin-android")
}
