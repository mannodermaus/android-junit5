package de.mannodermaus.gradle.plugins.junit5.util

import de.mannodermaus.gradle.plugins.junit5.internal.android
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

enum class Type {
  Unset, Application, Library, Feature, DynamicFeature
}

class TestProjectFactory2(private val environment: TestEnvironment2) {

  fun newRootProject(): Project {
    // Pre-configure a "local.properties" file
    // containing the required location of the Android SDK
    val rootProject = ProjectBuilder.builder().build()

    rootProject.file("local.properties").writer().use {
      it.write("sdk.dir=${environment.androidSdkFolder.absolutePath}")
    }

    return rootProject
  }

  fun newProject(parent: Project, name: String? = null) = TestProjectBuilder(parent, name)

  inner class TestProjectBuilder(parent: Project, name: String?) {

    private var projectType = Type.Unset
    private var appId = "com.example.android"
    private var applyJUnit5Plugin = true
    private var applyJacocoPlugin = false
    private var applyKotlinPlugin = false

    private val project = ProjectBuilder.builder()
        .withParent(parent)
        .run {
          if (name != null) {
            this.withName(name)
          }

          build()
        }

    fun asAndroidApplication() = setProjectTypeIfUnsetTo(Type.Application)

    fun asAndroidFeature() = setProjectTypeIfUnsetTo(Type.Feature)

    fun asAndroidDynamicFeature() = setProjectTypeIfUnsetTo(Type.DynamicFeature)

    fun asAndroidLibrary() = setProjectTypeIfUnsetTo(Type.Library)

    fun applyJUnit5Plugin(state: Boolean = true) = apply {
      this.applyJUnit5Plugin = state
    }

    fun applyJacocoPlugin(state: Boolean = true) = apply {
      this.applyJacocoPlugin = state
    }

    fun applyKotlinPlugin(state: Boolean = true) = apply {
      this.applyKotlinPlugin = state
    }

    fun build(): Project {
      // Write out required Android file structure
      project.file(".").mkdir()
      project.file("src/main").mkdirs()

      val manifestFile = project.file("src/main/AndroidManifest.xml")
      if (!manifestFile.exists()) {
        manifestFile.writeText(androidManifestString())
      }

      // Apply required plugins
      project.applyPlugin(when (projectType) {
        Type.Application -> "com.android.application"
        Type.Feature -> "com.android.feature"
        Type.DynamicFeature -> "com.android.dynamic-feature"
        Type.Library -> "com.android.library"
        else -> throw IllegalArgumentException("Project type must be set")
      })

      if (applyJacocoPlugin) {
        project.applyPlugin("jacoco")
      }

      if (applyKotlinPlugin) {
        project.applyPlugin("kotlin-android")
      }

      if (applyJUnit5Plugin) {
        project.applyPlugin("de.mannodermaus.android-junit5")
      }

      // Add default configuration
      project.android.apply {
        compileSdkVersion(environment.compileSdkVersion)
        buildToolsVersion(environment.buildToolsVersion)
      }

      if (projectType == Type.Application) {
        project.android.defaultConfig.apply {
          applicationId = appId
          minSdkVersion(environment.minSdkVersion)
          targetSdkVersion(environment.targetSdkVersion)
          versionCode = 1
          versionName = "1.0"
        }
      }

      return project
    }

    fun buildAndEvaluate(): Project {
      val project = build()
      project.evaluate()
      return project
    }

    /* Private */

    private fun setProjectTypeIfUnsetTo(type: Type) = apply {
      if (projectType != Type.Unset) {
        throw IllegalArgumentException("Project type already set to $projectType")
      }

      this.projectType = type
    }

    private fun androidManifestString() =
        """
        <manifest
            xmlns:android="schemas.android.com/apk/res/android"
            package="$appId">
        </manifest>
    """.trimIndent()
  }
}
