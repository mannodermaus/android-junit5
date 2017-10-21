package de.mannodermaus.gradle.plugins.android_junit5

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestPlugin
import org.gradle.api.Project

class ProjectConfig {

  final Project project

  ProjectConfig(Project project) {
    this.project = project
  }

  boolean isAndroidPluginApplied() {
    return project.plugins.findPlugin(AppPlugin.class) || project.plugins.findPlugin(
        TestPlugin.class) || isAndroidLibraryPluginApplied()
  }

  boolean isAndroidLibraryPluginApplied() {
    return project.plugins.findPlugin(LibraryPlugin.class)
  }

  boolean isJacocoPluginApplied() {
    return project.plugins.findPlugin("jacoco")
  }

  boolean isKotlinPluginApplied() {
    return project.plugins.findPlugin("kotlin-android")
  }
}
