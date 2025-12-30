package de.mannodermaus.gradle.plugins.junit5.util

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration

/* Methods */

fun assertThat(actual: Project): ProjectSubject = Truth.assertAbout(::ProjectSubject).that(actual)

/* Types */

class ProjectSubject(metadata: FailureMetadata, private val actual: Project?) :
    Subject(metadata, actual) {

    fun configuration(name: String): ConfigurationSubject =
        check("configuration()")
            .about(::ConfigurationSubject)
            .that(actual?.configurations?.getByName(name))

    fun task(name: String): TaskSubject =
        check("task()").about(::TaskSubject).that(actual?.tasks?.findByName(name))
}

class ConfigurationSubject(metadata: FailureMetadata, private val actual: Configuration?) :
    Subject(metadata, actual) {
    private val dependencyNames by lazy {
        actual?.dependencies?.map { "${it.group}:${it.name}:${it.version}" }.orEmpty()
    }

    fun hasDependency(notation: String) {
        containsDependency(notation, expectExists = true)
    }

    fun doesNotHaveDependency(notation: String) {
        containsDependency(notation, expectExists = false)
    }

    /* Private */

    private fun containsDependency(notation: String, expectExists: Boolean) {
        // If the expected dependency has a version component,
        // include it in the check. Otherwise, check for the existence
        // of _any_ version for the dependency in question
        val notationIncludesVersion = notation.count { it == ':' } > 1
        val hasMatch =
            if (notationIncludesVersion) {
                notation in dependencyNames
            } else {
                dependencyNames.any { it.startsWith("$notation:") }
            }

        val messagePrefix =
            if (expectExists) {
                "Expected to have a dependency on '$notation' in configuration '${actual?.name}', but did not."
            } else {
                "Expected not to have a dependency on '$notation' in configuration '${actual?.name}', but did."
            }

        assertWithMessage("$messagePrefix\nDependencies in this configuration: $dependencyNames")
            .that(hasMatch)
            .isEqualTo(expectExists)
    }
}

class TaskSubject(metadata: FailureMetadata, private val actual: Task?) :
    Subject(metadata, actual) {
    fun exists() {
        assertThat(actual).isNotNull()
    }

    fun doesNotExist() {
        assertThat(actual).isNull()
    }
}
