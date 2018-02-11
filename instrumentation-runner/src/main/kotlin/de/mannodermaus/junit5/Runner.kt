package de.mannodermaus.junit5

import org.junit.platform.runner.JUnitPlatform

/**
 * JUnit Runner implementation using the JUnit Platform as its backbone.
 * Serves as an intermediate solution to writing JUnit 5-based instrumentation tests
 * until official support arrives for this.
 *
 * Suppressing unused, since this class is instantiated reflectively.
 *
 * Replacement For:
 * AndroidJUnit4
 */
@Suppress("unused")
internal class AndroidJUnit5(klass: Class<*>) : JUnitPlatform(klass)
