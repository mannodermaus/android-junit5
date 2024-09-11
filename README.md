<!--
  This file was automatically generated by Gradle. Do not modify.
  To update the content of this README, please apply modifications
  to `README.md.template` instead, and run the `generateReadme` task from Gradle.
-->
# <img src=".images/logo.png" align="right" width="100">android-junit5 [![CircleCI](https://circleci.com/gh/mannodermaus/android-junit5/tree/main.svg?style=svg)][circleci]

A Gradle plugin that allows for the execution of [JUnit 5][junit5gh] tests in Android environments using **Android Gradle Plugin 8.0.0 or later.**

## How?

This plugin configures the unit test tasks for each build variant of a project to run on the JUnit Platform. Furthermore, it provides additional configuration options for these tests [through a DSL][wiki-dsl] and facilitates the usage of JUnit 5 for instrumentation tests.

Instructions on how to write tests with the JUnit 5 framework can be found [in their User Guide][junit5ug]. To get a first look at its features, a small showcase project can be found [here][sampletests].

## Setup

To get started, declare the plugin in your `app` module's build script alongside the latest version. Snapshots of the development version are available through [Sonatype's `snapshots` repository][sonatyperepo].

<details open>
  <summary>Kotlin</summary>

  ```kotlin
  plugins {
    id("de.mannodermaus.android-junit5") version "1.11.0.0"
  }

  dependencies {
    // (Required) Writing and executing Unit Tests on the JUnit Platform
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")

    // (Optional) If you need "Parameterized Tests"
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")

    // (Optional) If you also have JUnit 4-based tests
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.0")
  }
  ```
</details>

<details>
  <summary>Groovy</summary>

  ```groovy
  plugins {
    id "de.mannodermaus.android-junit5" version "1.11.0.0"
  }

  dependencies {
    // (Required) Writing and executing Unit Tests on the JUnit Platform
    testImplementation "org.junit.jupiter:junit-jupiter-api:5.11.0"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:5.11.0"

    // (Optional) If you need "Parameterized Tests"
    testImplementation "org.junit.jupiter:junit-jupiter-params:5.11.0"

    // (Optional) If you also have JUnit 4-based tests
    testImplementation "junit:junit:4.13.2"
    testRuntimeOnly "org.junit.vintage:junit-vintage-engine:5.11.0"
  }
  ```
</details>

<br/>

### Alternative: Legacy DSL

If you prefer to use the legacy way to declare the dependency instead, remove the `version()` block from above and declare the plugin in your _root project's build script_ like so:

<details>
  <summary>Kotlin</summary>

  ```kotlin
  // In the root project's build.gradle.kts:
  buildscript {
    dependencies {
      classpath("de.mannodermaus.gradle.plugins:android-junit5:1.11.0.0")
    }
  }

  // In the app module's build.gradle.kts:
  plugins {
    id("de.mannodermaus.android-junit5")
  }
  ```
</details>

<details>
  <summary>Groovy</summary>

  ```groovy
  // In the root project's build.gradle:
  buildscript {
    dependencies {
      classpath "de.mannodermaus.gradle.plugins:android-junit5:1.11.0.0"
    }
  }

  // In the app module's build.gradle:
  apply plugin: "de.mannodermaus.android-junit5"
  ```
</details>

<br/>

More information on Getting Started can be found [on the wiki][wiki-gettingstarted].

## Requirements

The latest version of this plugin requires:
* Android Gradle Plugin `8.0.0` or above
* Gradle `8.0` or above

## Instrumentation Test Support

You can use JUnit 5 to run instrumentation tests on emulators and physical devices, too. Because the framework is built on Java 8 from the ground up, these instrumentation tests will only run on devices running Android 8.0 (API 26) or newer – older phones will skip the execution of these tests completely, marking them as "ignored".

Before you can write instrumentation tests with JUnit Jupiter, make sure that your module is using the `androidx.test.runner.AndroidJUnitRunner` (or a subclass of it) as its `testInstrumentationRunner`. Then, simply add a dependency on `junit-jupiter-api` to the `androidTestImplementation` configuration in your build script and the plugin will automatically configure JUnit 5 tests for you:

<details open>
  <summary>Kotlin</summary>
  
  ```kotlin
  dependencies {
    androidTestImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
  }
  ```
</details>

<details>
  <summary>Groovy</summary>

  ```groovy
  dependencies {
    androidTestImplementation "org.junit.jupiter:junit-jupiter-api:5.11.0"
  }
  ```
</details>

By enabling JUnit 5 for instrumentation tests, you will gain access to `ActivityScenarioExtension` (among other things), which helps with the orchestration of `Activity` classes. Check [the wiki][wiki-home] for more info.

### Extensions

An optional artifact with more helper extensions is available for specific use cases.
It contains the following APIs:

- `GrantPermissionExtension` for granting permissions before each test

Can you think of more? Let's discuss in the issues section!

<details open>
  <summary>Kotlin</summary>

  ```kotlin
  junitPlatform {
    instrumentationTests.includeExtensions.set(true)
  }
  ```
</details>

<details>
  <summary>Groovy</summary>

  ```groovy
  junitPlatform {
    instrumentationTests.includeExtensions.set(true)
  }
  ```
</details>

### Jetpack Compose

To test `@Composable` functions on device with JUnit 5, first enable support for instrumentation tests as described above.
Then, add the Compose test dependency to your `androidTestImplementation` configuration
and the plugin will autoconfigure JUnit 5 Compose support for you!

<details open>
  <summary>Kotlin</summary>
  
  ```kotlin
  dependencies {
    // Compose test framework
    androidTestImplementation("androidx.compose.ui:ui-test-android:$compose_version")

    // Needed for createComposeExtension() and createAndroidComposeExtension()
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
  }
  ```
</details>

<details>
  <summary>Groovy</summary>

  ```groovy
  dependencies {
    // Compose test framework
    androidTestImplementation "androidx.compose.ui:ui-test-android:$compose_version"

    // Needed for createComposeExtension() and createAndroidComposeExtension()
    debugImplementation "androidx.compose.ui:ui-test-manifest:$compose_version"
  }
  ```
</details>

[The wiki][wiki-home] includes a section on how to test your Composables with JUnit 5.

### Override the version of instrumentation test libraries

By default, the plugin will make sure to use a compatible version of the instrumentation test libraries
when it sets up the artifacts automatically. However, it is possible to choose a custom version instead via its DSL:

<details open>
  <summary>Kotlin</summary>

  ```kotlin
  junitPlatform {
    instrumentationTests.version.set("1.5.0")
  }
  ```
</details>

<details>
  <summary>Groovy</summary>

  ```groovy
  junitPlatform {
    instrumentationTests.version.set("1.5.0")
  }
  ```
</details>

## Official Support

At this time, Google hasn't shared any immediate plans to bring first-party support for JUnit 5 to Android. The following list is an aggregation of pending feature requests:

- [InstantTaskExecutorRule uses @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) -- why? (issuetracker.google.com)](https://issuetracker.google.com/u/0/issues/79189568)
- [Add support for JUnit 5 (issuetracker.google.com)](https://issuetracker.google.com/issues/127100532)
- [JUnit 5 support (github.com/android/android-test)](https://github.com/android/android-test/issues/224)

## Support for @Rules

Since JUnit 5 has replaced the `@Rule` mechanism with Extensions, the following artifacts help bridge the gap until Android officially transitions to JUnit 5.

### InstantExecutorExtension

Replaces `InstantTaskExecutorRule` in JUnit 5.

<details>
  <summary>Kotlin</summary>

  ```kotlin
  dependencies {
      testImplementation("io.github.neboskreb:instant-task-executor-extension:1.0.0")
  }
  ```
</details>

<details>
  <summary>Groovy</summary>

  ```groovy
  dependencies {
      testImplementation 'io.github.neboskreb:instant-task-executor-extension:1.0.0'
  }
  ```
</details>

For more details see [instant-task-executor-extension](https://github.com/neboskreb/instant-task-executor-extension) on GitHub.

## Building Locally

This repository contains multiple modules, divided into two sub-projects. The repository's root directory contains build logic shared across the sub-projects, which in turn use symlinks to connect to the common build scripts in their parent folder.

- `instrumentation`: The root folder for Android-based modules, namely the instrumentation libraries & a sample application. After cloning, open this project in Android Studio.
- `plugin`: The root folder for Java-based modules, namely the Gradle plugin for JUnit 5 on Android, as well as its test module. After cloning, open this project in IntelliJ IDEA.

## Plugin Compatibility Map

For users that cannot match the current minimum version requirement of the Android Gradle Plugin requested by this plugin,
refer to the table below to find a suitable alternative version. Note that **no active development will go into these
legacy versions**, so please consider upgrading to at least Android Gradle Plugin 8.0.0
before filing an issue with the latest one.

|Your AGP Version|Suggested JUnit5 Plugin Version|
|---|---|
|`7.0.0` - `7.4.2`|`1.10.0.0`|
|`4.0.0` - `4.2.2`|`1.8.2.1`|
|`3.5.0` - `3.6.4`|`1.7.1.1`|
|`< 3.5.0`|none; you should **really** update your build env, bro|

## License

```
Copyright 2017-2024 Marcel Schnelle

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
 [circleci]: https://circleci.com/gh/mannodermaus/android-junit5
 [sonatyperepo]: https://oss.sonatype.org/content/repositories/snapshots
 [sampletests]: instrumentation/sample
 [wiki-home]: https://github.com/mannodermaus/android-junit5/wiki
 [wiki-dsl]: https://github.com/mannodermaus/android-junit5/wiki/Configuration
 [wiki-gettingstarted]: https://github.com/mannodermaus/android-junit5/wiki/Getting-Started
