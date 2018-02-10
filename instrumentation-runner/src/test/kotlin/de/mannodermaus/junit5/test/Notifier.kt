package de.mannodermaus.junit5.test

import org.junit.runner.Description
import org.junit.runner.notification.RunListener

class CountingRunListener : RunListener() {

  private val methodNames = mutableListOf<String>()

  override fun testFinished(description: Description) {
    // Only count actual method executions
    // (this method is also called for the class itself)
    description.methodName?.let { methodNames += it }
  }

  fun count() = this.methodNames.size

  fun methodNames() = methodNames.toList()
}
