package de.mannodermaus.junit5.condition

import de.mannodermaus.junit5.internal.DisabledIfBuildConfigValueCondition
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DisabledIfBuildConfigValueCondition::class)
public annotation class DisabledIfBuildConfigValue(
    val named: String,
    val matches: String
)
