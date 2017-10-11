# android-junit5

[![Travis Build Status](https://travis-ci.org/mannodermaus/android-junit5.svg?branch=master)][travisci]

A Gradle plugin that allows for the execution of [JUnit 5][junit5gh] tests in Android environments.

## Download

```groovy
buildscript {
  dependencies {
    classpath "de.mannodermaus.gradle.plugins:android-junit5:1.0.10"
  }
}
```

Snapshots of the development version are available through [Sonatype's `snapshots` repository][sonatyperepo].

## Setup

```groovy
apply plugin: "com.android.application"
apply plugin: "de.mannodermaus.android-junit5"

dependencies {
  testImplementation junit5()

  // (Optional) If you need "parameterized tests"
  testImplementation junit5Params()
    
  // (Optional) For running tests inside Android Studio 3.x (see below for details)
  testCompileOnly junit5EmbeddedRuntime()
}
```

## Usage

This plugin configures a `junitPlatformTest` task for each registered build variant of a project.
It automatically attaches both the Jupiter & Vintage Engines during the execution phase of your tests as well.

More instructions on how to write JUnit 5 tests can be found [in their User Guide][junit5ug].
Furthermore, this repository provides a small showcase of the functionality provided by JUnit 5 [here][sampletests].

## Android Studio Workarounds

> **Note:**
> 
> The following section deals with fixing Test Execution within **Android Studio 3**.
> Running your JUnit 5 tests directly from Android Studio 2.3.3 and earlier **will not work**:
> You will encounter an `AbstractMethodError` when trying to do so ([more information here][as2issue]).
> 
> The cause of this error is similar in nature to the one described below, and related to outdated APIs.
> Unlike that issue though, we can't fix the `AbstractMethodError` inside IntelliJ's internal runtime
> in the same way. Therefore, please resort to using Gradle for unit testing in Android Studio 2.


All versions up to and including **Android Studio 3.0 Beta 7** are built
on a version of IntelliJ IDEA that depends on outdated JUnit 5 APIs.
Therefore, your tests will fail with an Exception similar to the following when you try to
launch your tests from inside the IDE (using an *Android JUnit* Run Configuration):

```
Exception in thread "main" java.lang.NoSuchMethodError: org.junit.platform.launcher.Launcher.execute(Lorg/junit/platform/launcher/LauncherDiscoveryRequest;)V
	at com.intellij.junit5.JUnit5IdeaTestRunner.startRunnerWithArgs(JUnit5IdeaTestRunner.java:42)
	...
```

To work around this, there is a separate dependency you can add to the *test* scope
of your project in Android Studio 3. It provides its own copy of the JUnit 5 Runtime
provided by a more recent build of IntelliJ, overriding the one embedded in Android Studio.

To use this, add the following line alongside the other `junit5()` dependencies:

```groovy
dependencies {
  testCompileOnly junit5EmbeddedRuntime()
}
```

## Extras

### Override Dependency Versions

Inside the configuration closure applied by the plugin, you can specify the same properties as you would
for a Java-based project with the JUnit Platform Gradle plugin.
However, there are some additional properties that you can apply:

```groovy
junitPlatform {
  // The JUnit Jupiter dependency version to use; matches the platform's version by default
  jupiterVersion "5.0.1"
  // The JUnit Vintage Engine dependency version to use; matches the platform's version by default
  vintageVersion "4.12.1"
}
```

### JaCoCo Integration

If the plugin detects the usage of [JaCoCo][jacoco] inside a project that it's being applied to,
it will automatically configure additional tasks to report the unit test coverage
of your application based on its JUnit 5 tests.
There is no additional setup required to enable this behaviour.
You can however customize the reports JaCoCo should generate.

Configuration is applied through the `jacoco` clause inside the plugin's DSL:

```groovy
apply plugin: "jacoco"

junitPlatform {
  jacoco {
    csvReport true
    xmlReport true
    htmlReport true
  }
}
```

## License

`android-junit5` is distributed with multiple Open Source licenses:

- `android-junit5-embedded-runtime` uses [Apache License v2.0](android-junit5-embedded-runtime/LICENSE.md)
- All other modules use [Eclipse Public License v2.0](android-junit5/LICENSE.md)

Please see the `LICENSE.md` files in the subfolders for more details.

 [junit5gh]: https://github.com/junit-team/junit5
 [junit5ug]: http://junit.org/junit5/docs/current/user-guide
 [travisci]: https://travis-ci.org/mannodermaus/android-junit5
 [as2issue]: https://github.com/mannodermaus/android-junit5/issues/19
 [jacoco]: http://www.eclemma.org/jacoco
 [sonatyperepo]: https://oss.sonatype.org/content/repositories/snapshots/de/mannodermaus/gradle/plugins
 [sampletests]: sample/src/test
 [licensefile]: LICENSE.md
