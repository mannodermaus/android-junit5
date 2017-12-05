package de.mannodermaus.gradle.plugins.junit5

import org.gradle.api.Task
import org.gradle.process.JavaForkOptions
import org.gradle.process.ProcessForkOptions

/*
 * Marker interface to allow the Kotlin-based tasks
 * to be referenced from Groovy indirectly.
 */

interface JUnit5UnitTest extends Task, JavaForkOptions, ProcessForkOptions {}
