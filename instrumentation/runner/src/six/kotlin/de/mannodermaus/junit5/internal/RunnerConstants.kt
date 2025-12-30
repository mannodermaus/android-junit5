package de.mannodermaus.junit5.internal

/**
 * The minimum Android API level on which JUnit Framework tests may be executed. Trying to launch a
 * test on an older device will simply mark it as 'skipped'.
 */
internal const val JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION: Int = 35
