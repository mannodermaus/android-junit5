package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.SelectorsExtension

/*
 * Special Extension Methods for accessors
 * that need to reach into Groovy because of the
 * unfortunate lack of visibility modifiers in the main JUnit 5 Gradle Plugin,
 * which prevents the static typing of Kotlin from working properly.
 */

fun SelectorsExtension.isEmpty(): Boolean =
    GroovyInterop.selectorsExtension_isEmpty(this)

fun FiltersExtension.getIncludeClassNamePatterns(): List<String> =
    GroovyInterop.filtersExtension_includeClassNamePatterns(this)

fun FiltersExtension.getExcludeClassNamePatterns(): List<String> =
    GroovyInterop.filtersExtension_excludeClassNamePatterns(this)

val BaseVariant.variantData: BaseVariantData
  get() = GroovyInterop.baseVariant_variantData(this)
