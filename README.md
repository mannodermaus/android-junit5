# android-junit5

[![Travis Build Status](https://travis-ci.org/aurae/android-junit5.svg?branch=master)][travisci]

A Gradle plugin that allows for the execution of [JUnit 5][junit5gh] unit tests in Android environments.

## Download

```groovy
buildscript {
    dependencies {
        classpath "de.mannodermaus.gradle.plugins:android-junit5:1.0.0-RC3-rev1"
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
    
    // For Android Studio users:
    //
    // All versions up to and including AS 3.0 Beta 5 use a build of IntelliJ IDEA
    // that depends on APIs of an outdated Milestone Release of JUnit 5.
    // Therefore, your tests will fail with a NoSuchMethodError
    // when executed from Android Studio directly.
    //
    // To prevent this, there is a separate library you can apply here.
    // It provides a copy of the JUnit 5 Runtime used in a more recent build
    // of IntelliJ, overriding the one embedded in Android Studio.
    testCompileOnly "de.mannodermaus.gradle.plugins:android-junit5-embedded-runtime:1.0.0-RC3-rev1"
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
    jupiterVersion "5.0.0-RC3"
    // The JUnit Vintage Engine dependency version to use; matches the platform's milestone by default
    vintageVersion "4.12.0-RC3"
}
```

 [junit5gh]: https://github.com/junit-team/junit5
 [junit5ug]: http://junit.org/junit5/docs/current/user-guide
 [travisci]: https://travis-ci.org/aurae/android-junit5
 [sonatyperepo]: https://oss.sonatype.org/content/repositories/snapshots
 [sampletests]: https://github.com/aurae/android-junit5/tree/master/sample/src/test
