package de.mannodermaus.gradle.plugins.junit5.dsl

/**
 * How to react when instrumentation tests are executed on a device that doesn't meet the minSdk
 * requirement imposed by the JUnit Framework instrumentation libraries. By default, these tests are
 * silently skipped; set it to [Fail] to raise an error and abort test execution instead.
 */
public enum class UnsupportedDeviceBehavior(internal val value: String) {
    Skip("skip"),
    Fail("fail"),
}
