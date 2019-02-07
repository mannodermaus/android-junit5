package de.mannodermaus.gradle.plugins.junit5

import com.android.annotations.NonNull
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import com.annimon.stream.Optional
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.testing.jacoco.tasks.JacocoReportBase

import javax.annotation.Nullable

/**
 * Utility functions exposed to Kotlin consumers
 * that can't safely access Groovy members otherwise,
 * or require reflection to access in a compatible manner
 * across all supported versions of the Android Gradle Plugin.*/
class GroovyInterop {

  //   Access to UNIT_TEST was moved from VariantType to VariantTypeImpl in AGP 3.2.0-alpha06
  private static final def VariantType =
      reflectiveClass("com.android.builder.core.VariantTypeImpl")
          .or { reflectiveClass("com.android.builder.core.VariantType") }
  // Java outputs are accessed through this enum in AGP 3.2.0-alpha02
  private static final def InternalArtifactType =
      reflectiveClass("com.android.build.gradle.internal.scope.InternalArtifactType")

  // No instances
  private GroovyInterop() { throw new AssertionError() }

  /**
   * Attempts to look up a Class based on its FQCN, returns an empty Optional if this fails
   * @param fqcn Fully qualified class name
   * @return The class, or an empty Optional
   */
  private static Optional<Class> reflectiveClass(String fqcn) {
    try {
      return Optional.of(Class.forName(fqcn))
    } catch (ignored) {
      return Optional.empty()
    }
  }

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
   * Obtains the Java output directory of the provided VariantScope in a safe manner.
   * TODO Clean this mess up once the Android Gradle Plugin 3.2.0 finally decides on something. :|
   *
   * @because In Android Gradle Plugin 3.2.0-alpha02, the original method was removed
   * @param variant VariantScope to retrieve the Java output directory from
   * @return That file
   */
  @NonNull
  static Set<File> variantScope_javaOutputDirs(VariantScope scope) {
    if (scope.hasProperty("buildArtifactsHolder") && InternalArtifactType.isPresent()) {
      def artifactType = InternalArtifactType
          .map { it.getDeclaredField("JAVAC").get(null) }
          .get()
      if (scope.buildArtifactsHolder.hasArtifact(artifactType)) {
        // 3.2.0-alpha04 and above:
        // Java outputs are moved into a subdirectory exposed by the compilation BuildArtifact
        return scope.buildArtifactsHolder.getArtifactFiles(artifactType).files
      } else {
        // 3.2.0-alpha02 and above:
        // Java outputs are still inside the "intermediates/classes" directory,
        // but there is no public API for that, so construct the path yourself
        return [new File(scope.globalScope.intermediatesDir,
            "/classes/" + scope.variantConfiguration.dirName)]
      }
    } else {
      // Below 3.2.0-alpha02:
      // Java outputs are expressed through the javaOutputDir property
      return [scope.javaOutputDir]
    }
  }

  /**
   * Obtains the Assets Collection of the given AndroidUnitTest.
   *
   * @because 'assetsCollection' type changed from FileCollection to BuildArtifact in Android Gradle Plugin 3.2.0-alpha07
   * @param test The Android JUnit 4 test to access
   * @return Its assets collection
   */
  @Nullable
  static Set<File> androidUnitTest_assetsCollection(AndroidUnitTest test) {
    def collection = test.assetsCollection
    return collection == null ? null : collection.files
  }

  /**
   * Obtains the Res Collection of the given AndroidUnitTest.
   *
   * @because 'resCollection' type changed from FileCollection to BuildArtifact in Android Gradle Plugin 3.2.0-alpha11
   * @param test The Android JUnit 4 test to access
   * @return Its assets collection
   */
  @Nullable
  static Set<File> androidUnitTest_resCollection(AndroidUnitTest test) {
    def collection = test.resCollection
    return collection == null ? null : collection.files
  }

  /**
   * Obtains the Merged Manifest of the given AndroidUnitTest.
   *
   * @because 'mergedManifest' type changed from FileCollection to BuildArtifact in Android Gradle Plugin 3.2.0-alpha07
   * @param test The Android JUnit 4 test to access
   * @return Its merged manifest
   */
  @Nullable
  static Set<File> androidUnitTest_mergedManifest(AndroidUnitTest test) {
    def collection = test.mergedManifest
    return collection == null ? null : collection.files
  }

  /**
   * Obtains the task name prefix for Unit Test variants.
   *
   * @because In Android Gradle Plugin 3.2.0-alpha06, the underlying constants on VariantType were renamed
   * @return The unit test prefix
   */
  @NonNull
  static String variantType_unitTestPrefix() {
    return VariantType
        .map { it.getDeclaredField("UNIT_TEST").get(null) }
        .map { it.prefix }
        .orElseThrow { new IllegalArgumentException("can't get VariantType.UNIT_TEST.prefix") }
  }

  /**
   * Obtains the task name suffix for Unit Test variants.
   *
   * @because In Android Gradle Plugin 3.2.0-alpha06, the underlying constants on VariantType were renamed
   * @return The unit test prefix
   */
  @NonNull
  static String variantType_unitTestSuffix() {
    return VariantType
        .map { it.getDeclaredField("UNIT_TEST").get(null) }
        .map { it.suffix }
        .orElseThrow { new IllegalArgumentException("can't get VariantType.UNIT_TEST.suffix") }
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
    if (report.executionData instanceof ConfigurableFileCollection) {
      // In Gradle 5.0+, executionData is a ConfigurableFileCollection
      report.executionData.setFrom(paths)
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
    if (report.classDirectories instanceof ConfigurableFileCollection) {
      // In Gradle 5.0+, executionData is a ConfigurableFileCollection
      report.classDirectories.setFrom(paths)
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
    if (report.sourceDirectories instanceof ConfigurableFileCollection) {
      // In Gradle 5.0+, executionData is a ConfigurableFileCollection
      report.sourceDirectories.setFrom(paths)
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
