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

  private final Project project

  AndroidJUnitPlatformExtension(Project project) {
    super(project)
    this.project = project
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

  /* Integration of Instrumentation Tests */

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

  /* Integration of Jacoco Reporting */

  /**
   * Options for controlling Jacoco reporting.*/
  private final JacocoOptions jacoco = new JacocoOptions(project)

  /**
   * Configures Jacoco reporting options.*/
  void jacoco(Closure closure) {
    ConfigureUtil.configure(closure, jacoco)
  }

  /**
   * Configures Jacoco reporting options.*/
  JacocoOptions getJacoco() { return jacoco }

  /**
   * Options for controlling Jacoco reporting.*/
  static class JacocoOptions {

    private final Project project
    private final Report html
    private final Report csv
    private final Report xml

    JacocoOptions(Project project) {
      this.project = project
      this.html = new Report()
      this.csv = new Report()
      this.xml = new Report()
    }

    void html(Closure closure) {
      ConfigureUtil.configure(closure, html)
    }

    Report getHtml() {
      return html
    }

    void csv(Closure closure) {
      ConfigureUtil.configure(closure, csv)
    }

    Report getCsv() {
      return csv
    }

    void xml(Closure closure) {
      ConfigureUtil.configure(closure, xml)
    }

    Report getXml() {
      return xml
    }

    // FIXME DEPRECATED ---------------------------------------------------------------
    def htmlReport(boolean state) {
      logDeprecationWarning("htmlReport", "html.enabled")
      html.enabled = state
    }

    def csvReport(boolean state) {
      logDeprecationWarning("csvReport", "csv.enabled")
      csv.enabled = state
    }

    def xmlReport(boolean state) {
      logDeprecationWarning("xmlReport", "xml.enabled")
      xml.enabled = state
    }

    private def logDeprecationWarning(String dontUse, String useInstead) {
      LogUtils.agpStyleLog(project.logger,
          LogUtils.Level.WARNING,
          "Accessing the Jacoco property '$dontUse' for JUnit 5 configuration " + "is deprecated and will be removed in a future version. Please use '$useInstead' instead")
    }

    // END DEPRECATED -----------------------------------------------------------------

    class Report {

      private boolean enabled = true

      @Nullable
      private File destination

      void enabled(boolean state) {
        this.enabled = state
      }

      boolean isEnabled() {
        return enabled
      }

      void destination(File destination) {
        this.destination = destination
      }

      @Nullable
      File getDestination() {
        return destination
      }
    }
  }
}
