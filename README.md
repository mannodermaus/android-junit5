# android-junit5

[![Travis Build Status](https://travis-ci.org/aurae/android-junit5.svg?branch=master)][travisci]

A Gradle plugin that allows for the execution of [JUnit 5][junit5gh] unit tests in Android environments.

## Setup

```groovy
buildscript {
    repositories {
        // 1. Make sure to include jcenter in your repositories
        jcenter()
    }
    dependencies {
        // 2. Add the plugin as a classpath dependency
        classpath "de.mannodermaus.gradle.plugins:android-junit5:1.0.0-M4-rev3"
    }
}

// 3. Apply the plugin below your Android plugin
apply plugin: "com.android.application"
apply plugin: "de.mannodermaus.android-junit5"

dependencies {
    // 4. Add the testCompile dependencies on JUnit Jupiter
    testCompile junitJupiter()

    // 5. (Optional) If you need "parameterized tests"
    testCompile junitParams()
}
```

## Usage

This plugin configures the `junitPlatform` task for each registered build variant of a project.
It automatically attaches both the Jupiter & Vintage Engines during the execution phase of your tests.

Further instructions on how to write JUnit 5 tests can be found [in their User Guide][junit5ug].

## Extras

Inside the configuration closure applied by the plugin, you can specify the same properties as you would
for a Java-based project with the JUnit Platform Gradle plugin.
However, there are some additional properties that you can apply:

```groovy
junitPlatform {
    // The JUnit Jupiter dependency version to use; "5.0.0-M4" by default
    jupiterVersion "5.0.0-M4"
    // The JUnit Vintage Engine dependency version to use; "4.12.0-M4" by default
    vintageVersion "4.12.0-M4"
}
```

 [junit5gh]: https://github.com/junit-team/junit5
 [junit5ug]: http://junit.org/junit5/docs/current/user-guide
 [travisci]: https://travis-ci.org/aurae/android-junit5
