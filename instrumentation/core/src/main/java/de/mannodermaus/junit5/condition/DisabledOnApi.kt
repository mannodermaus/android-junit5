package de.mannodermaus.junit5.condition

import androidx.annotation.IntRange
import de.mannodermaus.junit5.condition.EnabledOnApiCondition.NOT_SET
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DisabledOnApiCondition::class)
annotation class DisabledOnApi(@IntRange(from = 24) val min: Int = NOT_SET,
                               @IntRange(from = 24) val max: Int = NOT_SET)
