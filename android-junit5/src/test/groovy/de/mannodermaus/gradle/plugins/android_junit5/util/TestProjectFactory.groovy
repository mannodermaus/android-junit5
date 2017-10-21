package de.mannodermaus.gradle.plugins.android_junit5.util

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class TestProjectFactory {

  private final TestEnvironment environment

  TestProjectFactory(TestEnvironment environment) {
    this.environment = environment
  }

  Project newRootProject() {
    // Pre-configure a "local.properties" file
    // containing the required location of the Android SDK
    def p = ProjectBuilder.builder().build()
    p.file("local.properties").withWriter {
      it.write("sdk.dir=${environment.androidSdkFolder.absolutePath}")
    }
    return p
  }

  TestProjectBuilder newProject(Project parent, String name = null) {
    return new TestProjectBuilder(parent, name)
  }

  class TestProjectBuilder {

    enum Type {
      UNSET, APPLICATION, LIBRARY
    }

    private final Project project

    private Type projectType = Type.UNSET
    private String appId = "com.example.android"
    private boolean applyJunit5Plugin = true
    private boolean applyJacocoPlugin = false
    private boolean applyKotlinPlugin = false

    private TestProjectBuilder(Project parent, String name = null) {
      def builder = ProjectBuilder.builder().withParent(parent)

      if (name != null) {
        builder.withName(name)
      }

      this.project = builder.build()
    }

    /* Public */

    TestProjectBuilder asAndroidApplication() {
      if (projectType != Type.UNSET) {
        throw new IllegalArgumentException("Project type already set to $projectType")
      }

      projectType = Type.APPLICATION
      return this
    }

    TestProjectBuilder asAndroidLibrary() {
      if (projectType != Type.UNSET) {
        throw new IllegalArgumentException("Project type already set to $projectType")
      }

      projectType = Type.LIBRARY
      return this
    }

    TestProjectBuilder applyJunit5Plugin(boolean state = true) {
      applyJunit5Plugin = state
      return this
    }

    TestProjectBuilder applyJacocoPlugin(boolean state = true) {
      applyJacocoPlugin = state
      return this
    }

    TestProjectBuilder applyKotlinPlugin(boolean state = true) {
      applyKotlinPlugin = state
      return this
    }

    Project build() {
      // Write out required Android file structure
      project.file(".").mkdir()
      project.file("src/main").mkdirs()

      def manifestFile = project.file("src/main/AndroidManifest.xml")
      if (!manifestFile.exists()) {
        manifestFile.withWriter { it.write(androidManifestString()) }
      }

      // Apply required plugins
      switch (projectType) {
        case Type.APPLICATION:
          project.apply plugin: "com.android.application"
          break

        case Type.LIBRARY:
          project.apply plugin: "com.android.library"
          break
      }

      if (applyJunit5Plugin) {
        project.apply plugin: "de.mannodermaus.android-junit5"
      }

      if (applyJacocoPlugin) {
        project.apply plugin: "jacoco"
      }

      if (applyKotlinPlugin) {
        project.apply plugin: "kotlin-android"
      }

      // Default configuration
      project.android {
        compileSdkVersion environment.compileSdkVersion
        buildToolsVersion environment.buildToolsVersion
      }

      if (projectType == Type.APPLICATION) {
        project.android.defaultConfig {
          applicationId appId
          minSdkVersion environment.minSdkVersion
          targetSdkVersion environment.targetSdkVersion
          versionCode 1
          versionName "1.0"
        }
      }

      return project
    }

    Project buildAndEvaluate() {
      Project project = build()
      project.evaluate()
      return project
    }

    /* Private */

    private String androidManifestString() {
      return """
        <manifest
            xmlns:android="schemas.android.com/apk/res/android"
            package="$appId">
        </manifest>
    """
    }
  }
}
