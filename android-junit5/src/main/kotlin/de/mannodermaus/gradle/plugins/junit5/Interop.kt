package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.tasks.factory.AndroidUnitTest
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

val VariantScope.safeJavaOutputDirs: Set<File>
  get() = GroovyInterop.variantScope_javaOutputDirs(this)

val AndroidUnitTest.safeResCollection: Set<File>?
  get() = GroovyInterop.androidUnitTest_resCollection(this)

val AndroidUnitTest.safeAssetsCollection: Set<File>?
  get() = GroovyInterop.androidUnitTest_assetsCollection(this)

val AndroidUnitTest.safeMergedManifest: Set<File>?
  get() = GroovyInterop.androidUnitTest_mergedManifest(this)

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

/* Types */

class VariantTypeCompat {
  companion object {
    val UNIT_TEST_PREFIX = GroovyInterop.variantType_unitTestPrefix()
    val UNIT_TEST_SUFFIX = GroovyInterop.variantType_unitTestSuffix()
  }
}
