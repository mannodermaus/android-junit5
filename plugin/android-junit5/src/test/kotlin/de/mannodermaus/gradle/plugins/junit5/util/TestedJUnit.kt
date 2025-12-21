package de.mannodermaus.gradle.plugins.junit5.util

enum class TestedJUnit(val envPropName: String) {
    JUnit5("JUNIT5_VERSION"),
    JUnit6("JUNIT6_VERSION")
}
