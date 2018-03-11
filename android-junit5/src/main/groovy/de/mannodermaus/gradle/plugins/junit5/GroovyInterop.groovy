package de.mannodermaus.gradle.plugins.junit5

import com.android.annotations.NonNull
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData

/**
 * Utility functions exposed to Kotlin consumers
 * that can't safely access Groovy members otherwise.*/
class GroovyInterop {

  // No instances
  private GroovyInterop() { throw new AssertionError() }

  /**
   * Obtains the VariantData of the provided Variant.
   * BaseVariant#variantData is protected
   * and can't be accessed through Kotlin directly.
   *
   * @param variant Variant to retrieve the scope of
   * @return The scope
   */
  @NonNull
  static BaseVariantData baseVariant_variantData(BaseVariant variant) {
    return variant.variantData
  }

  /**
   * Obtains the Java output directory of the provided VariantScope in a safe manner.
   * In Android Gradle Plugin 3.2.0-alpha02, the original method was removed.
   *
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
}
