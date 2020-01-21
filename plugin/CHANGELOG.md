Change Log
==========

## Unreleased

#### Added
#### Changed
#### Fixed
#### Removed

## 1.5.2.0 (2019-10-09)

- JUnit 5.5.2

## 1.5.1.0 (2019-08-07)

- JUnit 5.5.1
- Fix compatibility with latest iterations of AGP 3.6.x (#180)

## 1.5.0.0 (2019-07-14)

- JUnit 5.5.0

#### Added
- Integrity checks for users including instrumentation tests into their project. An error will be thrown on incomplete setup of the instrumentation test environment. To disable the check, use the junitPlatform.instrumentationTests.integrityCheckEnabled API
- Added Gradle tasks to generate resource files for filter configurations in instrumentation tests (support @Tag filters in instrumentation tests)
#### Changed
- Use Dokka for platform-agnostic generation of documentation files
#### Removed
- Marked junitPlatform.instrumentationTests.enabled and junitPlatform.instrumentationTests.version as deprecated, as they don't do anything anymore - they are scheduled for removal in 1.6.0.0

## 1.4.2.1 (2019-06-12)

- Fix compatibility with the latest versions of the Android Gradle Plugin

## 1.4.2.0 (2019-04-08)

- JUnit 5.4.2

## 1.4.1.0 (2019-04-07)

- JUnit 5.4.1

## 1.4.0.0 (2019-03-03)

- JUnit 5.4.0

## 1.3.2.0 (2018-12-31)

- JUnit 5.3.2

#### Added
- Support for projects running Gradle 5.x (#131, #133)
#### Changed
- Converted all helper classes for unit tests to Kotlin & inverted the dependency between Kotlin and Groovy. Now, the only Groovy thing left in that particular module are some unit tests (#136)
- Moved to a type-safe way to declare libraries & versions in Kotlin DSL, instead of relying on the extra API in Gradle (#137)
#### Fixed
- Sample project correctly sets up its source folders (#135; thanks, @pardom!)
#### Removed
- The plugin no longer tries to configure modules that use the com.android.test plugin. These modules only use instrumentation tests, and do not expose any unit test tasks. If you want JUnit 5 in these modules, use the instrumentation test libraries instead (#134)

## 1.3.1.1 (2018-11-04)

- Improve usability of the plugin for users of the Kotlin DSL for Gradle

## 1.3.1.0 (2018-10-28)

- JUnit 5.3.1
- Add support for `com.android.dynamic-feature` plugin (#115)
- Fix DSL issue in projects with multi-dimensional product flavors (#110)
- Add support for AGP 3.3.0 alphas

## 1.3.0.0 (2018-10-28)

- JUnit 5.3.0

## 1.2.0.0 (2018-10-11)

- JUnit 5.2.0
- Update versioning scheme: `JUnit 5.x.y` == `Plugin 1.x.y.0`
- Introduce new configuration DSL
- Remove dependency handlers
- Remove the need to create custom tasks - instead hook into the existing test tasks
- Remove most of the customized config DSLs and move towards a more streamlined filters API

## 1.0.33 (2018-10-12)

- Last release of the pre-3.2.0 line for backwards compatibility

## 1.0.32 (2018-05-03)

- Enhance compatibility with Android Gradle Plugin 3.2.0 alpha versions
- Remove dependency on Java-based JUnit 5 Gradle plugin
- Add warning on incorrect plugin application order with Kotlin
- JaCoCo now configures the source directories
- Add DSL for JUnit 5 unit tests related to default values & Android resources

## 1.0.31 (2018-03-03)

- Raise minimum required Gradle version to 4.3
- Add several DSL functions for configuration
- Remove deprecated `jacoco` container

## 1.0.30 (2018-01-22)

- JUnit 5.0.3
- Rename `jacoco` DSL to `jacocoOptions`

## 1.0.22 (2017-12-04)

#### Added

- Add initial support for instrumentation tests
- Add DSL & dependency handler for instrumentation tests

#### Fixed

- Unable to find method during gradle sync (#34)
- Running tests on multiple flavors (#36)
- Move junitPlatform configuration into android namespace enhancement (#37)
- Delete duplicated Jacoco config, obey the default (#38)

## 1.0.21 (2017-11-22)

- No notes

## 1.0.20 (2017-11-20)

- JUnit 5.0.2
- Replace the Copy task for Kotlin-based unit tests
- Rewrite plugin to almost-100% Kotlin

## 1.0.12 (2017-11-02)

#### Fixed

- Product Flavor tasks don't find their (Kotlin) tests properly (#25)

## 1.0.11 (2017-10-21)

- No notes

## 1.0.10 (2017-10-10)

- JUnit 5.0.1
- New versioning scheme: `JUnit 5.x.y` == `Plugin 1.x.y0`
- Add JaCoCo support for plugin structure
- Add dependency handler for embedded runtime artifact

## 1.0.0 (2017-09-11)

- JUnit 5.0.0
- Removal of deprecated dependency handlers

## 1.0.0-RC3-rev1 (2017-09-09)

- Rename dependency handlers provided by the plugin: `junitJupiter()` to `junit5()` & `junitParams()` to `junit5Params()`
- Introduce `android-junit5-embedded-runtime` artifact to bridge incompatibilities with IDE runtimes

## 1.0.0-RC3 (2017-08-29)

- Support `configurationParameters` in DSL

## 1.0.0-RC2 (2017-08-28)

- Dependency update

## 1.0.0-M6 (2017-07-19)

- Dependency update

## 1.0.0-M5 (2017-07-08)

- Improve backwards compatibility between Android Gradle Plugin & IDE runtimes
- Introduce snapshot builds

## 1.0.0-M4-rev3 (2017-06-11)

- Improve classpath compatibility & inconsistent execution behavior between IDE and command-line builds

## 1.0.0-M4-rev2 (2017-06-04)

- Improve compatibility with Android Gradle Plugin 3.0.0
- Add junitParams() dependency handler for parameterized tests
- Hopefully mitigate classpath-related issues when running unit tests from the IDE

## 1.0.0-M4-rev1 (2017-05-19)

- Added compatibility with Android Gradle Plugin 3.0.0 versions
- Included JUnit 4 by default, which allows AS builds w/o “class not found” errors
- Deprecated junitVintage() dependency handler, issuing a warning if you're still using it
- Both Jupiter & Vintage TestEngines are included on the runtime classpath by default

## 1.0.0-M4 (2017-05-03)

- Dependency update

## 1.0.0-M2 (2016-09-30)

- Initial release
