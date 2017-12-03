package de.mannodermaus.gradle.plugins.junit5.integrations

import com.android.builder.model.ProductFlavor
import de.mannodermaus.gradle.plugins.junit5.Callable1
import de.mannodermaus.gradle.plugins.junit5.JUNIT5_RUNNER_BUILDER_CLASS_NAME
import de.mannodermaus.gradle.plugins.junit5.PARAM_NAME_ENABLE_INSTRUMENTED_TESTS
import de.mannodermaus.gradle.plugins.junit5.RUNNER_BUILDER_ARG
import de.mannodermaus.gradle.plugins.junit5.android
import de.mannodermaus.gradle.plugins.junit5.ext
import de.mannodermaus.gradle.plugins.junit5.safeDefaultConfig
import org.gradle.api.Project

/**
 * Extends the default Android plugin models
 * to allow for configuration of parameters
 * related to the Instrumentation Test companion library.
 */
fun Project.attachInstrumentationTestSupport() {
  // Attach extensions to both the default configuration
  // as well as all product flavors, so that users can
  // enable Instrumentation Test support for the scope they want
  this.android.safeDefaultConfig.attachConfigureMethod()

  // Can't merge this with the default config b/c
  // we have to use "all()" here to auto-configure
  // any lazily appended flavor
  this.android.productFlavors.all { it.attachConfigureMethod() }
}

private fun ProductFlavor.attachConfigureMethod() {
  this.ext[PARAM_NAME_ENABLE_INSTRUMENTED_TESTS] = Callable1<Boolean, Unit> { enabled ->
    if (enabled) {
      this.testInstrumentationRunnerArguments[RUNNER_BUILDER_ARG] = JUNIT5_RUNNER_BUILDER_CLASS_NAME
    } else {
      this.testInstrumentationRunnerArguments.remove(RUNNER_BUILDER_ARG)
    }
  }
}
