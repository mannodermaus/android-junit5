package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(EnabledIfBuildConfigValueCondition::class)
annotation class EnabledIfBuildConfigValue(val named: String, val matches: String)
