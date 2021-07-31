package de.mannodermaus.junit5.internal

import android.annotation.TargetApi
import android.os.Build
import de.mannodermaus.junit5.condition.EnabledOnSdkVersion
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils.findAnnotation
import org.junit.platform.commons.util.Preconditions

internal class EnabledOnSdkVersionCondition : ExecutionCondition {

    companion object {
        private val ENABLED_BY_DEFAULT =
            ConditionEvaluationResult.enabled("@EnabledOnSdkVersion is not present")

        fun enabled(): ConditionEvaluationResult {
            return ConditionEvaluationResult.enabled("Enabled on API " + Build.VERSION.SDK_INT)
        }

        fun disabled(): ConditionEvaluationResult {
            return ConditionEvaluationResult.disabled("Disabled on API " + Build.VERSION.SDK_INT)
        }
    }

    @TargetApi(24)
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val optional = findAnnotation(context.element, EnabledOnSdkVersion::class.java)

        if (optional.isPresent) {
            val annotation = optional.get()
            val fromApi = annotation.from
            val untilApi = annotation.until
            val hasLowerBound = fromApi != NOT_SET
            val hasUpperBound = untilApi != NOT_SET
            Preconditions.condition(
                hasLowerBound || hasUpperBound,
                "At least one value must be provided in @EnabledOnSdkVersion"
            )

            // Constrain the current API Level based on the presence of "fromApi" & "untilApi":
            // If either one is not set at all, that part of the conditional becomes true automatically
            val lowerCheck = !hasLowerBound || Build.VERSION.SDK_INT >= fromApi
            val upperCheck = !hasUpperBound || Build.VERSION.SDK_INT <= untilApi
            return if (lowerCheck && upperCheck) {
                enabled()
            } else {
                disabled()
            }
        }

        return ENABLED_BY_DEFAULT
    }
}
