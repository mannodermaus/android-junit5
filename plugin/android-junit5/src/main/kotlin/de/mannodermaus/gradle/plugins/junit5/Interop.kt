package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import org.gradle.api.Project
import org.gradle.testing.jacoco.tasks.JacocoReportBase
import java.io.File

/*
 * Special Extension Methods for accessors
 * that need to reach into Groovy because of the
 * unfortunate lack of visibility modifiers in the main JUnit 5 Gradle Plugin,
 * which prevents the static typing of Kotlin from working properly.
 */

val BaseVariant.variantData: BaseVariantData
  get() = GroovyInterop.baseVariant_variantData(this)

val BaseVariant.safeJavaCompileDestinationDir: File
get() = GroovyInterop.baseVariant_javaCompileDestinationDir(this)

/*
 * Compatibility methods for multiple Gradle versions.
 */

fun JacocoReportBase.safeExecutionDataSetFrom(project: Project, vararg paths: Any) {
  GroovyInterop.jacocoReportBase_executionData_setFrom(this, project, paths)
}

fun JacocoReportBase.safeClassDirectoriesSetFrom(project: Project, vararg paths: Any) {
  GroovyInterop.jacocoReportBase_classDirectories_setFrom(this, project, paths)
}

fun JacocoReportBase.safeSourceDirectoriesSetFrom(project: Project, vararg paths: Any) {
  GroovyInterop.jacocoReportBase_sourceDirectories_setFrom(this, project, paths)
}

val JacocoReportBase.safeGetExecutionData get() =
  GroovyInterop.jacocoReportBase_getExecutionData(this)

val JacocoReportBase.safeGetSourceDirectories get() =
  GroovyInterop.jacocoReportBase_getSourceDirectories(this)

val JacocoReportBase.safeGetClassDirectories get() =
  GroovyInterop.jacocoReportBase_getClassDirectories(this)

/* Types */

class VariantTypeCompat {
  companion object {
    val UNIT_TEST_PREFIX = GroovyInterop.variantType_unitTestPrefix()
    val UNIT_TEST_SUFFIX = GroovyInterop.variantType_unitTestSuffix()
  }
}
