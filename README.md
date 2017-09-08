# android-junit5

[![Travis Build Status](https://travis-ci.org/aurae/android-junit5.svg?branch=master)][travisci]

A Gradle plugin that allows for the execution of [JUnit 5][junit5gh] unit tests in Android environments.

## Download

```groovy
buildscript {
    dependencies {
        classpath "de.mannodermaus.gradle.plugins:android-junit5:1.0.0-M6"
    }
}
```

Snapshots of the development version are available through [Sonatype's `snapshots` repository][sonatyperepo].

**Note**: This plugin also supports the Release Candidates of JUnit 5.
However, as of Android Studio 3.0 Beta 5, there is an internal issue with how tests are executed from the IDE
related to a removed API in JUnit 5 still accessed from Android Studio's build of IntelliJ.
If you run your JUnit 5 tests directly from Android Studio *right now*, they will fail with a `NoSuchMethodError`.

If you are running the latest version of IDEA itself, or you only ever run tests from the command line,
it's safe to upgrade this plugin to the *actual* most recent version, `1.0.0-RC3`.

## Setup

```groovy
apply plugin: "com.android.application"
apply plugin: "de.mannodermaus.android-junit5"

dependencies {
    testApi junitJupiter()

    // (Optional) If you need "parameterized tests"
    testApi junitParams()
}
```

## Usage

This plugin configures the `junitPlatform` task for each registered build variant of a project.
It automatically attaches both the Jupiter & Vintage Engines during the execution phase of your tests.

More instructions on how to write JUnit 5 tests can be found [in their User Guide][junit5ug].
Furthermore, this repository provides a small showcase of the functionality provided by JUnit 5 [here][sampletests].

## Extras

Inside the configuration closure applied by the plugin, you can specify the same properties as you would
for a Java-based project with the JUnit Platform Gradle plugin.
However, there are some additional properties that you can apply:

```groovy
junitPlatform {
    // The JUnit Jupiter dependency version to use; matches the platform's milestone by default
    jupiterVersion "5.0.0-M6"
    // The JUnit Vintage Engine dependency version to use; matches the platform's milestone by default
    vintageVersion "4.12.0-M6"
}
```

 [junit5gh]: https://github.com/junit-team/junit5
 [junit5ug]: http://junit.org/junit5/docs/current/user-guide
 [travisci]: https://travis-ci.org/aurae/android-junit5
 [sonatyperepo]: https://oss.sonatype.org/content/repositories/snapshots
 [sampletests]: https://github.com/aurae/android-junit5/tree/master/sample/src/test
