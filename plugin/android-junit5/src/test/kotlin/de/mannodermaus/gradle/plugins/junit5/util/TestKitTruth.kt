package de.mannodermaus.gradle.plugins.junit5.util

import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.IntegerSubject
import com.google.common.truth.StringSubject
import com.google.common.truth.Subject
import com.google.common.truth.Truth
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

/* Methods */

fun assertThat(actual: BuildResult): BuildResultSubject =
    Truth.assertAbout(::BuildResultSubject).that(actual)

/* Types */

class BuildResultSubject(
    metadata: FailureMetadata,
    private val actual: BuildResult?
) : Subject(metadata, actual) {

    fun task(name: String): BuildTaskSubject = check("task()")
        .about(::BuildTaskSubject)
        .that(actual?.task(name))

    fun output(): BuildResultOutputSubject = check("output()")
        .about(::BuildResultOutputSubject)
        .that(actual?.output)
}

class BuildTaskSubject(
    metadata: FailureMetadata,
    private val actual: BuildTask?
) : Subject(metadata, actual) {

    fun hasOutcome(expected: TaskOutcome) = check("hasOutcome()")
        .that(actual?.outcome)
        .isEqualTo(expected)
}

class BuildResultOutputSubject(
    metadata: FailureMetadata,
    private val actual: String?
) : StringSubject(metadata, actual) {

    fun ofTask(name: String): BuildTaskOutputSubject {
        requireNotNull(actual)

        val startIndex = actual.indexOf("> Task $name")
        if (startIndex == -1) {
            failWithActual(Fact.simpleFact("Task $name was not executed"))
        }

        var endIndex = actual.indexOf("> Task", startIndex + 1)
        if (endIndex == -1) {
            endIndex = actual.length - 1
        }

        val strippedOutput = actual.substring(startIndex, endIndex)
        return check("ofTask()")
            .about(::BuildTaskOutputSubject)
            .that(strippedOutput)
    }
}

class BuildTaskOutputSubject(
    metadata: FailureMetadata,
    private val actual: String?
) : StringSubject(metadata, actual) {

    fun executedTestCount(): IntegerSubject {
        requireNotNull(actual)

        // Subtract 1 from the total count because the task name is also preceded by ">"
        val actualCount = actual.count { it == '>' } - 1

        return check("executedTestCount()")
            .withMessage("actual test count: $actualCount. full task output: $actual")
            .that(actualCount)
    }
}
