package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DisabledIfBuildConfigValueCondition::class)
annotation class DisabledIfBuildConfigValue(val named: String, val matches: String)
