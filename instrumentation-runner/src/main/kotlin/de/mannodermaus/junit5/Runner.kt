package de.mannodermaus.junit5

import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.Runner

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

/**
 * Since we can't reference AndroidJUnit5 directly, use this factory for instantiation.
 */
internal fun createJUnit5Runner(klass: Class<*>): Runner = AndroidJUnit5(klass)
