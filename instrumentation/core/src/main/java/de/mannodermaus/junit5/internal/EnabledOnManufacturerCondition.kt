package de.mannodermaus.junit5.internal

import android.annotation.TargetApi
import android.os.Build
import de.mannodermaus.junit5.condition.EnabledOnManufacturer
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils.findAnnotation
import org.junit.platform.commons.util.Preconditions

internal class EnabledOnManufacturerCondition : ExecutionCondition {

    companion object {
        private val ENABLED_BY_DEFAULT =
            ConditionEvaluationResult.enabled("@EnabledOnManufacturer is not present")

        fun enabled(): ConditionEvaluationResult {
            return ConditionEvaluationResult.enabled("Enabled on Manufacturer: " + Build.MANUFACTURER)
        }

        fun disabled(): ConditionEvaluationResult {
            return ConditionEvaluationResult.disabled("Disabled on Manufacturer: " + Build.MANUFACTURER)
        }
    }

    @TargetApi(24)
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val optional = findAnnotation(context.element, EnabledOnManufacturer::class.java)

        if (optional.isPresent) {
            val annotation = optional.get()
            val patterns = annotation.value
            val ignoreCase = annotation.ignoreCase

            Preconditions.condition(
                patterns.isNotEmpty(),
                "You must declare at least one Manufacturer in @EnabledOnManufacturer"
            )

            return if (patterns.any { Build.MANUFACTURER.equals(it, ignoreCase = ignoreCase) }) {
                enabled()
            } else {
                disabled()
            }
        }

        return ENABLED_BY_DEFAULT
    }
}
