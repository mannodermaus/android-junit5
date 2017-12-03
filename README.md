# android-junit5

[![Travis Build Status](https://travis-ci.org/mannodermaus/android-junit5.svg?branch=master)][travisci]

A Gradle plugin that allows for the execution of [JUnit 5][junit5gh] tests in Android environments.

## Why a separate plugin?

The JUnit Platform team provides a Gradle plugin for running JUnit 5 on the JVM. However,
this plugin is tailored to the needs of a "purely Java" application, and doesn't work in
the context of the multi-variant world that we live in on Android. Therefore, this plugin was born.

It configures a `junitPlatformTest` task for each registered build variant of a project.
Furthermore, it automatically attaches both the Jupiter & Vintage Engines
during the execution phase of your tests as well, so there's very little configuration
necessary to get your project up-and-running on the JUnit Platform.

Instructions on how to write JUnit 5 tests can be found [in their User Guide][junit5ug].
Furthermore, this repository provides a small showcase of the functionality provided by JUnit 5 [here][sampletests].

## Download

```groovy
buildscript {
  dependencies {
    classpath "de.mannodermaus.gradle.plugins:android-junit5:1.0.22"
  }
}
```

Snapshots of the development version are available through [Sonatype's `snapshots` repository][sonatyperepo].

## Basic Setup

```groovy
apply plugin: "com.android.application"
apply plugin: "de.mannodermaus.android-junit5"

dependencies {
  // (Required) Writing and executing Unit Tests on the JUnit Platform.
  testImplementation junit5.unitTests()

  // (Optional) If you need "Parameterized Tests".
  testImplementation junit5.parameterized()
    
  // (Optional) For running tests inside Android Studio 3.x
  // Please refer to the "Android Studio Workarounds" section for more insight on this.
  testCompileOnly junit5.unitTestsRuntime()

  // (Optional) Writing and executing Instrumented Tests with the JUnit Platform Runner.
  //
  // IMPORTANT:
  // By declaring this dependency, you have to use a minSdkVersion
  // of at least 26, since the nature of JUnit 5 relies on APIs that aren't
  // available on Android devices before then.
  // Additionally, you are required to explicitly enable support for instrumented tests in the
  // "junitPlatform" configuration closure (see the section below for details).
  androidTestImplementation junit5.instrumentedTests()
}
```

## Configuration

The plugin applies a configuration closure to your module's `android.testOptions`.
Inside it, you can use [all properties available through the default JUnit 5 Gradle plugin][junit5config].
However, there are a few more parameters that allow for more customization of the JUnit Platform
in your Android project. These are detailed below, alongside their default values:

```groovy
android {
  testOptions {
    // Configuration closure added by the plugin;
    // all configurable parameters related to JUnit 5 can be found here
    junitPlatform {
      // The JUnit Jupiter dependency version to use
      jupiterVersion "5.0.2"

      // The JUnit Vintage Engine dependency version to use
      vintageVersion "4.12.2"

      // Whether or not JUnit 5 test tasks should be affected by
      // JVM Arguments, System Properties & Environment Variables
      // declared through "unitTests.all" closures
      applyDefaultTestOptions true

      // Options related to running instrumented tests with JUnit 5.
      // This is an incubating feature which utilizes the backwards-compatibility
      // of the JUnit Platform in order to extend the default Test Instrumentation Runner
      // with new power. Because of their experimental nature and minSdkVersion requirement,
      // they are turned off by default. If you enable them, you also have to specify
      // the library dependency in your androidTest scope. Please refer to the "Instrumented Tests"
      // section for more details.
      instrumentationTests {
        enabled false

        // The Android-Instrumentation-Test dependency version to use
        version "0.1.0"
      }

      // Configuration of companion tasks for JaCoCo Reports,
      // associated with each JUnit 5 task generated by the plugin.
      // Just like the companion tasks themselves, these properties
      // will only have an effect if your module declares the "jacoco" plugin as well.
      // For each of the available report types, you can toggle the availability
      // and destination folders that they write to.
      jacoco {
        xml {
          enabled true
          destination project.file()
        }
        html {
          enabled true
          destination project.file()
        }
        csv {
          enabled true
          destination project.file()
        }
      }
    }
  }
}
```

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


All versions up to and including **Android Studio 3.1 Canary** are built
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
  testCompileOnly junit5.unitTestsRuntime()
}
```

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
Copyright 2017 Marcel Schnelle

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
 [sonatyperepo]: https://oss.sonatype.org/content/repositories/snapshots/de/mannodermaus/gradle/plugins
 [sampletests]: sample/src/test
