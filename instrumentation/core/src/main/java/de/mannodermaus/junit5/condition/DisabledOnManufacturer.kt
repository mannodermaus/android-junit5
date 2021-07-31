package de.mannodermaus.junit5.condition

import de.mannodermaus.junit5.internal.DisabledOnManufacturerCondition
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DisabledOnManufacturerCondition::class)
public annotation class DisabledOnManufacturer(
    val value: Array<String>,
    val ignoreCase: Boolean = true
)
