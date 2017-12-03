package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Project

/**
 * Temporary Proxy class, used to promote the new way of specifying
 * JUnit Platform config parameters through android.testOptions,
 * while still allowing the "old way" to co-exist for a bit.*/
class ExtensionProxy {

  static def warning = "You're using the old way of configuring JUnit 5 in your project. " +
      "This is deprecated behavior and subject to removal in a future version of the plugin. " +
      "Please move your 'junitPlatform' clause into 'android.testOptions'!"

  private final Project project
  private final AndroidJUnitPlatformExtension delegate

  ExtensionProxy(Project project, AndroidJUnitPlatformExtension delegate) {
    this.project = project
    this.delegate = delegate
  }

  Object methodMissing(String methodName, Object args) {
    logWarning()
    return delegate.invokeMethod(methodName, args)
  }

  Object propertyMissing(String propertyName) {
    logWarning()
    return delegate.getProperty(propertyName)
  }

  def propertyMissing(String propertyName, Object value) {
    logWarning()
    delegate.setProperty(propertyName, value)
  }

  private def logWarning() {
    LogUtils.agpStyleLog(project.logger, LogUtils.Level.WARNING, warning)
  }
}
