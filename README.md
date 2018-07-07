# android-junit5 [![Travis Build Status](https://travis-ci.org/mannodermaus/android-junit5.svg?branch=master)][travisci]

![Logo](.images/logo.png)

A Gradle plugin that allows for the execution of [JUnit 5][junit5gh] tests in Android environments using **Android Gradle Plugin 3.0.0 or later.**

## Why a separate plugin?

The JUnit Platform team provides a Gradle plugin for running JUnit 5 on the JVM. However,
that plugin is tailored to the needs of a "purely Java" application, and doesn't work in
the context of the multi-variant world that we live in on Android. As a result, `android-junit5` was born.

This plugin configures a `junitPlatformTest` task for each registered build variant of a project.
Furthermore, it automatically attaches both the Jupiter & Vintage Engines
during the execution phase of your tests as well, so there's very little configuration
necessary to get your project up-and-running on the JUnit Platform.

Instructions on how to write JUnit 5 tests can be found [in their User Guide][junit5ug].
Furthermore, this repository provides a small showcase of the functionality provided by JUnit 5 [here][sampletests].

## Download

```groovy
buildscript {
  dependencies {
    classpath "de.mannodermaus.gradle.plugins:android-junit5:1.0.32"
  }
}
```

Snapshots of the development version are available through [Sonatype's `snapshots` repository][sonatyperepo].

## Setup

```groovy
apply plugin: "de.mannodermaus.android-junit5"

dependencies {
  // (Required) Writing and executing Unit Tests on the JUnit Platform.
  testImplementation junit5.unitTests()

  // (Optional) If you need "Parameterized Tests".
  testImplementation junit5.parameterized()

  // (Optional) Writing and executing Instrumented Tests with the JUnit Platform Runner.
  //
  // IMPORTANT:
  // By declaring this dependency, you have to use a minSdkVersion
  // of at least 26, since the nature of JUnit 5 relies on APIs that aren't
  // available on Android devices before then.
  // Consider creating a product flavor for this - see the sample project for details.
  androidTestImplementation junit5.instrumentationTests()
}
```

## Configuration

The plugin can be configured through a new configuration container inside `android.testOptions`.
Please check out the [Wiki page][wikiconfigpage] for an overview of the available DSL.

## Gradle Compatibility

The plugin's minimum required version of Gradle has increased over time to maximize its leverage with new APIs and performance.
The chart describes the evolution of this requirement. If you can't use the latest version of this plugin due to your
project's Gradle version, please refer to the following table to find the corresponding plugin that works for you.

|Plugin Version|Minimum Gradle Version|
|---|---|
|`1.0.30` and older|`2.5`|
|`1.0.31` and later|`4.3`|

## Licenses

#### android-junit5-embedded-runtime:

```
Copyright 2000-2016 JetBrains s.r.o.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See also the [full License text](android-junit5-embedded-runtime/LICENSE).

#### Everything else:

```
Copyright 2017-2018 Marcel Schnelle

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See also the [full License text](LICENSE).

 [junit5gh]: https://github.com/junit-team/junit5
 [junit5ug]: https://junit.org/junit5/docs/current/user-guide
 [junit5config]: http://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle-junit-configure
 [travisci]: https://travis-ci.org/mannodermaus/android-junit5
 [as2issue]: https://github.com/mannodermaus/android-junit5/issues/19
 [jacoco]: http://www.eclemma.org/jacoco
 [sonatyperepo]: https://oss.sonatype.org/content/repositories/snapshots
 [sampletests]: sample/src/test
 [wikiconfigpage]: https://github.com/mannodermaus/android-junit5/wiki/Configuration-DSL
