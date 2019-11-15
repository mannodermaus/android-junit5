package de.mannodermaus.junit5.condition

import androidx.annotation.IntRange
import de.mannodermaus.junit5.condition.EnabledOnSdkVersionCondition.NOT_SET
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(EnabledOnSdkVersionCondition::class)
annotation class EnabledOnSdkVersion(@IntRange(from = 24) val from: Int = NOT_SET,
                                     @IntRange(from = 24) val until: Int = NOT_SET)
