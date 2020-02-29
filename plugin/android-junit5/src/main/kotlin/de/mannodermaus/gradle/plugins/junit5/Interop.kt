package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.internal.invokeTyped
import de.mannodermaus.gradle.plugins.junit5.internal.reflectiveMethod
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testing.jacoco.tasks.JacocoReportBase
import java.io.File

/*
 * Special Extension Methods for accessors which need to be accessed in a reflective way.
 * Usually, these restrictions stem from version differences in Gradle or the Android Gradle Plugin.
 */

/*
 * =====================================================================================================================
 * BaseVariant
 * =====================================================================================================================
 */

/**
 * Obtains the Java class directory for the provided variant in a safe manner.
 *
 * @because Recent versions of the Android Gradle Plugin have moved to a Provider-based architecture, deprecating the old direct accessors for tasks.
 * @return That file
 */
val BaseVariant.safeJavaCompileDestinationDir: File
  get() {
    val javaCompileProvider = reflectiveMethod("getJavaCompileProvider")
    if (javaCompileProvider != null) {
      val provider = javaCompileProvider.invokeTyped<TaskProvider<JavaCompile>>(this)
      if (provider != null) {
        return provider.get().destinationDir
      }
    }

    return javaCompile.destinationDir
  }
/*
 * =====================================================================================================================
 * JacocoReportBase
 * =====================================================================================================================
 */

/**
 * Applies the given paths as the execution data of the given Jacoco task.
 *
 * @because Gradle 5 removed the ability to reassign this field directly, and replaced it with #setFrom()
 * @param project Project to resolve the paths if necessary
 * @param paths Paths to apply
 */
fun JacocoReportBase.safeExecutionDataSetFrom(project: Project, vararg paths: Any) {
  safeSetConfigurableFileCollection("executionData", project, *paths)
}

/**
 * Applies the given paths as the class directories of the given Jacoco task.
 *
 * @because Gradle 5 removed the ability to reassign this field directly, and replaced it with #setFrom()
 * @param project Project to resolve the paths if necessary
 * @param paths Paths to apply
 */
fun JacocoReportBase.safeClassDirectoriesSetFrom(project: Project, vararg paths: Any) {
  safeSetConfigurableFileCollection("classDirectories", project, *paths)
}

/**
 * Applies the given paths as the source directories of the given Jacoco task.
 *
 * @because Gradle 5 removed the ability to reassign this field directly, and replaced it with #setFrom()
 * @param project Project to resolve the paths if necessary
 * @param paths Paths to apply
 */
fun JacocoReportBase.safeSourceDirectoriesSetFrom(project: Project, vararg paths: Any) {
  safeSetConfigurableFileCollection("sourceDirectories", project, *paths)
}

/**
 * Obtains the execution data of a Jacoco task.
 *
 * @because Gradle 5 changed the return type of this field to expose the ConfigurableFileCollection directly
 * @return The execution data of the task
 */
val JacocoReportBase.safeGetExecutionData
  get() = safeGetConfigurableFileCollection("executionData")

/**
 * Obtains the source directories of a Jacoco task.
 *
 * @because Gradle 5 changed the return type of this field to expose the ConfigurableFileCollection directly
 * @return The source directories of the task
 */
val JacocoReportBase.safeGetSourceDirectories
  get() = safeGetConfigurableFileCollection("sourceDirectories")

/**
 * Obtains the class directories of a Jacoco task.
 *
 * @because Gradle 5 changed the return type of this field to expose the ConfigurableFileCollection directly
 * @return The class directories of the task
 */
val JacocoReportBase.safeGetClassDirectories
  get() = safeGetConfigurableFileCollection("classDirectories")

/* Private */

private fun JacocoReportBase.safeGetConfigurableFileCollection(name: String): FileCollection? {
  val method = reflectiveMethod("get${name.capitalize()}")
  return method?.invokeTyped<FileCollection>(this)
}

private fun JacocoReportBase.safeSetConfigurableFileCollection(name: String, project: Project, vararg paths: Any) {
  // Check if the getter function exists and is callable;
  // if so, we are running on Gradle 5.0 or newer, and can utilize
  // the new ConfigurableFileCollection APIs directly
  val method = reflectiveMethod("get${name.capitalize()}")
  if (method != null) {
    val collection = method.invokeTyped<FileCollection>(this)
    if (collection is ConfigurableFileCollection) {
      // In Gradle 5.0+, executionData is a ConfigurableFileCollection
      collection.setFrom(*paths)
      return
    }
  }

  // Below Gradle 5.0, use the mutator method to configure the data
  val setMethod = reflectiveMethod("set${name.capitalize()}", FileCollection::class.java)
  if (setMethod != null) {
    setMethod.invokeTyped<Void>(this, project.files(*paths))
  } else {
    throw IllegalArgumentException("JacocoReport.$name not available")
  }
}

/* Types */

class VariantTypeCompat {
  companion object {
    // Copied from:
    // com.android.builder.core.VariantType.Companion
    const val UNIT_TEST_PREFIX = "test"
    const val UNIT_TEST_SUFFIX = "UnitTest"
  }
}
