package de.mannodermaus.junit5

import android.util.Log
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier

/**
 * Created by Marcel Schnelle on 2019-03-16.
 */
class DummyJUnit5(private val testClass: Class<*>) : Runner() {

  private val testMethods = testClass.jupiterTestMethods()

  override fun run(notifier: RunNotifier) {
    Log.w(LOG_TAG, "JUnit 5 is not supported on this device. All Jupiter tests will be disabled.")

    for (testMethod in testMethods) {
      val description = Description.createTestDescription(testClass, testMethod.name)
      notifier.fireTestIgnored(description)
    }
  }

  override fun getDescription(): Description = Description.createSuiteDescription(testClass)
}
