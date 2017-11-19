package de.mannodermaus.gradle.plugins.junit5

import com.android.annotations.NonNull
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.VariantScope

/**
 * Utility functions exposed to Kotlin consumers
 * that can't safely access Groovy members otherwise.*/
class GroovyInterop {
  // No instances
  private GroovyInterop() { throw new AssertionError() }

  /**
   * Obtains the scope of the provided Variant.
   *
   * BaseVariant#variantData is protected
   * and can't be accessed through Kotlin directly.
   *
   * @param variant Variant to retrieve the scope of
   * @return The scope
   */
  @NonNull
  static VariantScope scopeOf(BaseVariant variant) {
    return variant.variantData.scope
  }
}
