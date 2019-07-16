package de.mannodermaus.junit5.condition

import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(EnabledOnManufacturerCondition::class)
annotation class EnabledOnManufacturer(val value: Array<String>, val ignoreCase: Boolean = true)
