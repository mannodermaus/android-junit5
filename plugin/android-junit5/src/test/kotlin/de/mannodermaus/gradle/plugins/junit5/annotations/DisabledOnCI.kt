package de.mannodermaus.gradle.plugins.junit5.annotations

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
annotation class DisabledOnCI
