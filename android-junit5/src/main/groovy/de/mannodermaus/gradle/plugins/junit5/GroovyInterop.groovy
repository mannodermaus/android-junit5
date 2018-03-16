package de.mannodermaus.gradle.plugins.junit5

import com.android.annotations.NonNull
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData

/**
 * Utility functions exposed to Kotlin consumers
 * that can't safely access Groovy members otherwise.*/
class GroovyInterop {

  // Access to UNIT_TEST was moved from VariantType to VariantTypeImpl in AGP 3.2.0-alpha06
  private static final Optional<Class> VariantType =
      reflectiveClass("com.android.builder.core.VariantTypeImpl")
          .orElseGet { reflectiveClass("com.android.builder.core.VariantType") }

  // No instances
  private GroovyInterop() { throw new AssertionError() }

  /**
   * Attempts to look up a Class based on its FQCN, returns an empty Optional if this fails
   * @param fqcn Fully qualified class name
   * @return The class, or an empty Optional
   */
  private static Optional<Class> reflectiveClass(fqcn) {
    try {
      return Optional.ofNullable(Class.forName(fqcn))
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
  static File variantScope_javaOutputDir(VariantScope scope) {
    if (scope.hasProperty("javaOutputDir")) {
      return scope.javaOutputDir
    } else {
      return new File(scope.globalScope.intermediatesDir,
          "/classes/" + scope.variantConfiguration.dirName)
    }
  }

  /**
   * Obtains the task name prefix for Unit Test variants.
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
