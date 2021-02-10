Change Log
==========

## Unreleased

#### Added
#### Changed
#### Fixed
#### Removed

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
