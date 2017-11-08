package de.mannodermaus.gradle.plugins.android_junit5

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

class ProjectConfig {

  private enum Type {
    APPLICATION("com.android.application", "applicationVariants"),
    TEST("com.android.test", "applicationVariants"),
    LIBRARY("com.android.library", "libraryVariants"),

    // Although there are "featureVariants" for modules applying the Feature plugin,
        // there is no distinct per-feature test task per se.
        // Therefore, we use the default library variants here
        FEATURE("com.android.feature", "libraryVariants")

    private final String pluginId
    private final String variantListName

    Type(String pluginId, String variantListName) {
      this.pluginId = pluginId
      this.variantListName = variantListName
    }

    static Type ofProject(Project project) throws ProjectConfigurationException {
      def type = values().find { project.plugins.findPlugin(it.pluginId) }
      if (type == null) {
        throw new ProjectConfigurationException("An Android plugin must be applied to this project",
            null)
      }
      return type
    }
  }

  final Project project
  final Type type

  ProjectConfig(Project project) {
    this.type = Type.ofProject(project)
    this.project = project
  }

  List<? super BaseVariant> getUnitTestVariants() {
    def allVariants = project.android[type.variantListName] as List<? super BaseVariant>
    return allVariants.findAll { it.hasProperty("unitTestVariant") }
  }

  boolean isJacocoPluginApplied() {
    return project.plugins.findPlugin("jacoco")
  }

  boolean isKotlinPluginApplied() {
    return project.plugins.findPlugin("kotlin-android")
  }
}
