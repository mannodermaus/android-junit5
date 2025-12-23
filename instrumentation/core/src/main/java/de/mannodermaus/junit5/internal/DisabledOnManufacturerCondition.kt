package de.mannodermaus.junit5.internal

import androidx.annotation.RequiresApi
import android.os.Build
import de.mannodermaus.junit5.condition.DisabledOnManufacturer
import de.mannodermaus.junit5.internal.EnabledOnManufacturerCondition.Companion.disabled
import de.mannodermaus.junit5.internal.EnabledOnManufacturerCondition.Companion.enabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils.findAnnotation
import org.junit.platform.commons.util.Preconditions

internal class DisabledOnManufacturerCondition : ExecutionCondition {

    companion object {
        private val ENABLED_BY_DEFAULT =
            ConditionEvaluationResult.enabled("@DisabledOnManufacturer is not present")
    }

    @RequiresApi(24)
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val optional = findAnnotation(context.element, DisabledOnManufacturer::class.java)

        if (optional.isPresent) {
            val annotation = optional.get()
            val patterns = annotation.value
            val ignoreCase = annotation.ignoreCase

            Preconditions.condition(
                patterns.isNotEmpty(),
                "You must declare at least one Manufacturer in @DisabledOnManufacturer"
            )

            return if (patterns.any { Build.MANUFACTURER.equals(it, ignoreCase = ignoreCase) }) {
                disabled()
            } else {
                enabled()
            }
        }
        return ENABLED_BY_DEFAULT
    }
}
