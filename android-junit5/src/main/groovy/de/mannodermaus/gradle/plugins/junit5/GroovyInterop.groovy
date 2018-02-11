package de.mannodermaus.gradle.plugins.junit5

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import org.gradle.api.tasks.TaskInputs
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.SelectorsExtension

import static java.util.Collections.emptyList

/**
 * Utility functions exposed to Kotlin consumers
 * that can't safely access Groovy members otherwise.*/
class GroovyInterop {
  // No instances
  private GroovyInterop() { throw new AssertionError() }

  /**
   * Add the provided key-value pair to the given TaskInputs object.
   * Gradle 4.3 included a binary-incompatible change to this method's
   * return type, which fails for clients running older versions of the
   * build system.
   *
   * Refer to the related issue on GitHub:
   * https://github.com/mannodermaus/android-junit5/issues/34
   *
   * FIXME Once the plugin's minimal Gradle version reaches 4.3, remove this.
   *
   * @param inputs TaskInputs to update
   * @param key Key of the property to set
   * @param value Value of the property to set
   * @return Self reference with a backwards-compatible type
   */
  @NonNull
  static TaskInputs taskInputs_safeProperty(TaskInputs inputs, String key, @Nullable Object value) {
    return inputs.property(key, value) as TaskInputs
  }

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
   * Obtains the "includeClassNamePatterns" property off a FiltersExtension.
   * This property doesn't have a visibility modifier in
   * the main Gradle plugin, and therefore needs
   * to be accessed in this fashion.
   *
   * @param extension Extension to access
   * @return The list of "include class name patterns"
   */
  @NonNull
  static List<String> filtersExtension_includeClassNamePatterns(FiltersExtension extension) {
    return extension.includeClassNamePatterns ?: emptyList()
  }

  /**
   * Obtains the "excludeClassNamePatterns" property off a FiltersExtension.
   * This property doesn't have a visibility modifier in
   * the main Gradle plugin, and therefore needs
   * to be accessed in this fashion.
   * ?: Collections.emptyList()
   * @param extension Extension to access
   * @return The list of "exclude class name patterns"
   */
  @NonNull
  static List<String> filtersExtension_excludeClassNamePatterns(FiltersExtension extension) {
    return extension.excludeClassNamePatterns ?: emptyList()
  }

  /**
   * Obtains the "isEmpty" property off a SelectorsExtension.
   * This property is protected in Groovy
   * and can't be accessed directly through Kotlin.
   *
   * @param extension Extension to access
   * @return Whether or not the Selectors are empty
   */
  static boolean selectorsExtension_isEmpty(SelectorsExtension extension) {
    return extension.empty
  }
}
