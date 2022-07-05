package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

internal inline fun <reified T : Task> TaskContainer.namedOrNull(name: String): TaskProvider<T>? =
    try {
        named(name, T::class.java)
    } catch (e: UnknownTaskException) {
        null
    }
