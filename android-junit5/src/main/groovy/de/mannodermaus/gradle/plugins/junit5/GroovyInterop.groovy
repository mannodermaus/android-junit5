package de.mannodermaus.gradle.plugins.junit5

import com.android.annotations.NonNull
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData
import com.annimon.stream.Optional

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
        // Java outputs are moved into an "artifacts_transform" subdirectory
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
}
