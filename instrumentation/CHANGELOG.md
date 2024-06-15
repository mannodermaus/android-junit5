Change Log
==========

## Unreleased

- Fix inheritance hierarchy of `ComposeExtension` to avoid false-positive warning regarding `@RegisterExtension` (#318)
- Improve parallel test execution for Android instrumentation tests
- Fix invalid naming of dynamic tests when executing only a singular test method from the IDE (#317, #339)
- Prevent test methods incorrectly defined as Kotlin top-level functions from messing up Android's internal test counting, causing issues like "Expected N+1 tests, received N" (#316)
- Prevent test classes ignored by a tag from being considered for test execution, causing issues like "Expected N+1 tests, received N" (#298)
- Improve integration with Android Test Orchestrator and remove the need for `@UseTechnicalNames` (#337)

## 1.4.0 (2023-11-05)

- Update formatting of instrumentation test names to prevent breaking generation of log files in newer versions of AGP (#263)
- Add support for test sharding (#270)
- Add support for inherited tests (#288)
- Only autoconfigure JUnit 5 for instrumentation tests when the user explicitly adds junit-jupiter-api as a dependency
- Prevent noisy logs in Logcat complaining about unresolvable annotation classes (#306)
- Add support for parallel execution of non-UI instrumentation tests (#295)
- Introduce `android-test-extensions` artifact with optional extensions, starting with a port of JUnit 4's `GrantPermissionRule` (#251)

## 1.3.0 (2021-09-17)

**This release of the instrumentation libraries requires JUnit 5.8.0 or newer. Please check your dependency declarations!**

#### Added
#### Changed
- Restructured and converted internal code of `core` and `runner` modules to 100% Kotlin
#### Fixed
- Running an individual test method with parameters will properly execute that test, even from the IDE (#199)
  (Note: Due to limitations with Android's instrumentation, this test would be reported without its parameters in the report)
- Running an individual test method with @DisplayName will properly execute that test, even from the IDE (#207)
  (Note: Due to limitations with Android's instrumentation, this test would be reported with its technical method name instead of the display name when executed in isolation)
#### Removed

## 1.2.2 (2021-03-02)

#### Fixed
- Include missing transitive runtime-only dependencies in generated POMs (side-effect of moving to a different deployment script)

## 1.2.1 (2021-02-10)

This version is identical to 1.2.0, but deployed to Maven Central to ensure availability after the upcoming shutdown of JCenter. 

## 1.2.0 (2019-11-20)

#### Added
- Conditional test execution: Control the execution of tests dynamically; constrain tests to run only when certain conditions are met. This includes support for most of the annotations provided by the main JUnit Jupier API, as well as some custom ones specifically for Android development! Check out the wiki for more information.
#### Fixed
- Execution of individual instrumentation tests works now
- Reporting of instrumentation tests uses improved naming schemes, preventing "mangled test names", esp. for parameterized and other non-default tests
#### Removed
- The source for the old & deprecated instrumentation artifacts (android-instrumentation-test and android-instrumentation-test-runner) have been removed

## 1.1.0 (2019-07-14)

#### Added
- Added support for @Tag filtering in androidTest methods & classes; this requires using Gradle Plugin 1.5.0.0 or newer to work!
#### Changed
- Replaced RunnerBuilder implementation for JUnit 5 to be less dependent on the JUnitPlatform Runner

## 1.0.0 (2019-04-08)

- Introduce new instrumentation libraries: `android-test-core` & `android-test-runner`
- Remove minSdk requirement; old devices simply skip JUnit 5 tests if their OS is too old
- Deprecate old libraries
- Introduce new API, centered around `ActivityScenario`

## 0.2.2 (2018-05-02)

- No notes

## 0.2.1 (2018-03-03)

- No notes

## 0.2.0 (2018-03-03)

- No notes

## 0.1.1 (2017-12-03)

- Initial release of experimental libraries
