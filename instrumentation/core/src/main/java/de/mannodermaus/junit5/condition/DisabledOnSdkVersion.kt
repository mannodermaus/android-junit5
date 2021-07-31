package de.mannodermaus.junit5.condition

import androidx.annotation.IntRange
import de.mannodermaus.junit5.JUNIT5_MINIMUM_SDK_VERSION
import de.mannodermaus.junit5.internal.DisabledOnSdkVersionCondition
import de.mannodermaus.junit5.internal.NOT_SET
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DisabledOnSdkVersionCondition::class)
public annotation class DisabledOnSdkVersion(
    @IntRange(from = JUNIT5_MINIMUM_SDK_VERSION.toLong()) val from: Int = NOT_SET,
    @IntRange(from = JUNIT5_MINIMUM_SDK_VERSION.toLong()) val until: Int = NOT_SET
)
