package de.mannodermaus.gradle.plugins.junit5.util

import com.google.common.truth.*
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

/* Methods */

fun assertThat(actual: BuildResult): BuildResultSubject = Truth.assertAbout(::BuildResultSubject).that(actual)

/* Types */

class BuildResultSubject(metadata: FailureMetadata, actual: BuildResult) : Subject<BuildResultSubject, BuildResult>(metadata, actual) {

  fun task(name: String): BuildTaskSubject = check().about(::BuildTaskSubject).that(actual().task(name))

  fun output(): BuildResultOutputSubject = check().about(::BuildResultOutputSubject).that(actual().output) as BuildResultOutputSubject
}

class BuildTaskSubject(metadata: FailureMetadata, actual: BuildTask) : Subject<BuildTaskSubject, BuildTask>(metadata, actual) {

  fun hasOutcome(expected: TaskOutcome) = check().that(actual().outcome).isEqualTo(expected)
}

class BuildResultOutputSubject(metadata: FailureMetadata, actual: String) : StringSubject(metadata, actual) {

  fun ofTask(name: String): BuildTaskOutputSubject {
    val actual = actual()
    val startIndex = actual.indexOf("> Task $name")
    if (startIndex == -1) {
      failWithActual(Fact.simpleFact("Task $name was not executed"))
    }

    var endIndex = actual.indexOf("> Task", startIndex + 1)
    if (endIndex == -1) {
      endIndex = actual.length - 1
    }

    val strippedOutput = actual.substring(startIndex, endIndex)
    return check().about(::BuildTaskOutputSubject).that(strippedOutput) as BuildTaskOutputSubject
  }
}

class BuildTaskOutputSubject(metadata: FailureMetadata, actual: String) : StringSubject(metadata, actual) {

  fun executedTestCount(): IntegerSubject {
    val actual = actual()

    // Subtract 1 from the total count because the task name is also preceded by ">"
    val actualCount = actual.count { it == '>' } - 1

    return check()
        .withMessage("actual test count: $actualCount. full task output: $actual")
        .that(actualCount)
  }
}
