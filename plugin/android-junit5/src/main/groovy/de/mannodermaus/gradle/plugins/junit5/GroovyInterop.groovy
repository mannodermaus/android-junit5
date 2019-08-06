package de.mannodermaus.gradle.plugins.junit5

import com.android.annotations.NonNull
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.core.VariantTypeImpl
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.testing.jacoco.tasks.JacocoReportBase

/**
 * Utility functions exposed to Kotlin consumers
 * that can't safely access Groovy members otherwise,
 * or require reflection to access in a compatible manner
 * across all supported versions of the Android Gradle Plugin.*/
class GroovyInterop {

  // No instances
  private GroovyInterop() { throw new AssertionError() }

  /**
   * Obtains the VariantData of the provided Variant.
   *
   * @because 'BaseVariant.variantData' is protected and can't be accessed through Kotlin directly
   * @param variant Variant to retrieve the scope of
   * @return The scope
   */
  @NonNull
  static BaseVariantData baseVariant_variantData(BaseVariant variant) {
    return variant.variantData
  }

  /**
   * Obtains the Java class directory for the provided variant in a safe manner.
   *
   * @because Recent versions of the Android Gradle Plugin have moved to a Provider-based architecture, deprecating the old direct accessors for tasks.
   * @param variant Variant to retrieve the destination directory for class files from
   * @return That file
   */
  @NonNull
  static File baseVariant_javaCompileDestinationDir(BaseVariant variant) {
    if (variant.hasProperty("javaCompileProvider")) {
      return variant.javaCompileProvider.get().destinationDir
    } else {
      return variant.javaCompile.destinationDir
    }
  }

  /**
   * Obtains the task name prefix for Unit Test variants.
   *
   * @because Kotlin cannot see the VariantTypeImpl class
   * @return The unit test task prefix
   */
  @NonNull
  static String variantType_unitTestPrefix() {
    return VariantTypeImpl.UNIT_TEST.prefix
  }

  /**
   * Obtains the task name suffix for Unit Test variants.
   *
   * @because Kotlin cannot see the VariantTypeImpl class
   * @return The unit test task prefix
   */
  @NonNull
  static String variantType_unitTestSuffix() {
    return VariantTypeImpl.UNIT_TEST.suffix
  }

  /**
   * Applies the given paths as the execution data of the given Jacoco task.
   *
   * @because Gradle 5 removed the ability to reassign this field directly, and replaced it with #setFrom()
   * @param report Report to operate on
   * @param project Project to resolve the paths if necessary
   * @param paths Paths to apply
   */
  static void jacocoReportBase_executionData_setFrom(JacocoReportBase report, Project project, Object... paths) {
    def val = jacocoReportBase_getExecutionData(report)
    if (val instanceof ConfigurableFileCollection) {
      // In Gradle 5.0+, executionData is a ConfigurableFileCollection
      val.setFrom(paths)
    } else {
      // Below, use the mutator method to configure the data
      report.executionData = project.files(paths)
    }
  }

  /**
   * Applies the given paths as the class directories of the given Jacoco task.
   *
   * @because Gradle 5 removed the ability to reassign this field directly, and replaced it with #setFrom()
   * @param report Report to operate on
   * @param project Project to resolve the paths if necessary
   * @param paths Paths to apply
   */
  static void jacocoReportBase_classDirectories_setFrom(JacocoReportBase report, Project project, Object... paths) {
    def val = jacocoReportBase_getClassDirectories(report)
    if (val instanceof ConfigurableFileCollection) {
      // In Gradle 5.0+, executionData is a ConfigurableFileCollection
      val.setFrom(paths)
    } else {
      // Below, use the mutator method to configure the data
      report.classDirectories = project.files(paths)
    }
  }

  /**
   * Applies the given paths as the source directories of the given Jacoco task.
   *
   * @because Gradle 5 removed the ability to reassign this field directly, and replaced it with #setFrom()
   * @param report Report to operate on
   * @param project Project to resolve the paths if necessary
   * @param paths Paths to apply
   */
  static void jacocoReportBase_sourceDirectories_setFrom(JacocoReportBase report, Project project, Object... paths) {
    def val = jacocoReportBase_getSourceDirectories(report)
    if (val instanceof ConfigurableFileCollection) {
      // In Gradle 5.0+, executionData is a ConfigurableFileCollection
      val.setFrom(paths)
    } else {
      // Below, use the mutator method to configure the data
      report.sourceDirectories = project.files(paths)
    }
  }

  /**
   * Obtains the execution data of a Jacoco task.
   *
   * @because Gradle 5 changed the return type of this field to expose the ConfigurableFileCollection directly
   * @param report Jacoco report task to operate on
   * @return The execution data of the task
   */
  static ConfigurableFileCollection jacocoReportBase_getExecutionData(JacocoReportBase report) {
    def val = report.executionData
    if (val instanceof ConfigurableFileCollection) {
      return val
    } else {
      return (ConfigurableFileCollection) val
    }
  }

  /**
   * Obtains the source directories of a Jacoco task.
   *
   * @because Gradle 5 changed the return type of this field to expose the ConfigurableFileCollection directly
   * @param report Jacoco report task to operate on
   * @return The source directories of the task
   */
  static ConfigurableFileCollection jacocoReportBase_getSourceDirectories(JacocoReportBase report) {
    def val = report.sourceDirectories
    if (val instanceof ConfigurableFileCollection) {
      return val
    } else {
      return (ConfigurableFileCollection) val
    }
  }

  /**
   * Obtains the class directories of a Jacoco task.
   *
   * @because Gradle 5 changed the return type of this field to expose the ConfigurableFileCollection directly
   * @param report Jacoco report task to operate on
   * @return The class directories of the task
   */
  static ConfigurableFileCollection jacocoReportBase_getClassDirectories(JacocoReportBase report) {
    def val = report.classDirectories
    if (val instanceof ConfigurableFileCollection) {
      return val
    } else {
      return (ConfigurableFileCollection) val
    }
  }
}
