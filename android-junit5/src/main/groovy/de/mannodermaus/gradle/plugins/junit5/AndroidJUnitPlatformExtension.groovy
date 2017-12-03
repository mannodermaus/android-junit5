package de.mannodermaus.gradle.plugins.junit5

import com.android.annotations.NonNull
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

import javax.annotation.Nullable

/*
 * Core configuration options for the Android JUnit 5 Gradle plugin.
 * This extends the functionality available through the Java-based JUnitPlatformExtension.
 *
 * Note: Because the inheritance chain of this class reaches into the JUnit 5 codebase
 * written in Groovy, this class cannot be easily translated into Kotlin.
 */

class AndroidJUnitPlatformExtension extends JUnitPlatformExtension {

  AndroidJUnitPlatformExtension(Project project) {
    super(project)
  }

  /** The version of JUnit Jupiter to use.*/
  @Nullable String jupiterVersion

  /** The version of JUnit Vintage Engine to use. */
  @Nullable
  String vintageVersion

  /**
   * Whether or not to apply the Android Gradle Plugin's "testOptions"
   * to JUnit 5 tasks - true by default.
   *
   * Note that this will only affect the following properties assigned
   * by a "testOptions.unitTests.all" closure:
   *
   * - jvmArgs
   * - systemProperties */
  boolean applyDefaultTestOptions = true

  /**
   * Options for controlling instrumentation test execution with JUnit 5.
   *
   * @since 1.0.22
   */
  private final InstrumentationTestOptions instrumentationTests = new InstrumentationTestOptions()

  /**
   * Configures instrumentation test options.
   *
   * @since 1.0.22
   */
  void instrumentationTests(Closure closure) {
    ConfigureUtil.configure(closure, instrumentationTests)
  }

  /**
   * Configures instrumentation test options.
   *
   * @since 1.0.22
   */
  @NonNull
  InstrumentationTestOptions getInstrumentationTests() { return instrumentationTests }

  /**
   * Options for controlling instrumentation test execution.*/
  static class InstrumentationTestOptions {

    /** Whether or not to enable support for JUnit 5 instrumentation tests. */
    boolean enabled = false

    /** The version of the instrumentation companion library to use. */
    @Nullable String version
  }
}
