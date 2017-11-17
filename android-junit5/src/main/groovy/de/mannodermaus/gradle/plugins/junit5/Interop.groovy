package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/**
 * Interoperability with Kotlin.
 * This class is used for Groovy-specific features
 * that haven't been mapped to Kotlin yet, e.g.
 * accessing dynamic properties like Gradle's ExtensionAware.*/
class Interop {

  static ExtensionAware createExtension(Object container, String name, Class cls) {
    return (ExtensionAware) container.extensions.create(name, cls)
  }

  static def createDependencyHandler(Project project, String name, config) {
    project.dependencies.ext[name] = { return config }
  }
}
