package de.mannodermaus.junit5.internal

import android.annotation.TargetApi
import de.mannodermaus.junit5.internal.utils.BuildConfigValueUtils
import de.mannodermaus.junit5.condition.EnabledIfBuildConfigValue
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils.findAnnotation
import org.junit.platform.commons.util.Preconditions

internal class EnabledIfBuildConfigValueCondition : ExecutionCondition {

    companion object {
        private val ENABLED_BY_DEFAULT =
            enabled("@EnabledIfBuildConfigValue is not present")
    }

    @TargetApi(24)
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val optional = findAnnotation(context.element, EnabledIfBuildConfigValue::class.java)

        if (optional.isPresent) {
            val annotation = optional.get()
            val name = annotation.named.trim()
            val regexString = annotation.matches

            Preconditions.notBlank(name) { "The 'named' attribute must not be blank in $annotation" }
            Preconditions.notBlank(regexString) { "The 'matches' attribute must not be blank in $annotation" }

            val actual = runCatching { BuildConfigValueUtils.getAsString(name) }.getOrNull()
                ?: return disabled("BuildConfig key [$name] does not exist")

            return if (actual.matches(regexString.toRegex())) {
                enabled("BuildConfig key [$name] with value [$actual] matches regular expression [$regexString]")
            } else {
                disabled("BuildConfig key [$name] with value [$actual] does not match regular expression [$regexString]")
            }
        }

        return ENABLED_BY_DEFAULT
    }
}
