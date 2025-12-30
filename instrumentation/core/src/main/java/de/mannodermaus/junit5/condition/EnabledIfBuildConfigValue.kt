package de.mannodermaus.junit5.condition

import de.mannodermaus.junit5.internal.EnabledIfBuildConfigValueCondition
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(EnabledIfBuildConfigValueCondition::class)
public annotation class EnabledIfBuildConfigValue(val named: String, val matches: String)
