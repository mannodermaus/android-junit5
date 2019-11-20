package de.mannodermaus.junit5.condition;

import android.annotation.TargetApi;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;

import java.util.Optional;

import static java.lang.String.format;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

class DisabledIfBuildConfigValueCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_BY_DEFAULT =
      enabled("@DisabledIfBuildConfigValue is not present");

  @TargetApi(24)
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<DisabledIfBuildConfigValue> optional = findAnnotation(context.getElement(), DisabledIfBuildConfigValue.class);

    if (optional.isPresent()) {
      DisabledIfBuildConfigValue annotation = optional.get();
      String name = annotation.named().trim();
      String regex = annotation.matches();
      Preconditions.notBlank(name, () -> "The 'named' attribute must not be blank in " + annotation);
      Preconditions.notBlank(regex, () -> "The 'matches' attribute must not be blank in " + annotation);
      String actual = getBuildConfigValue(name);
      
      // Nothing to match against?
      if (actual == null) {
        return enabled(format("BuildConfig key [%s] does not exist", name));
      }
      if (actual.matches(regex)) {
        return disabled(format("BuildConfig key [%s] with value [%s] matches regular expression [%s]",
            name, actual, regex));
      }
      return enabled(format("BuildConfig key [%s] with value [%s] does not match regular expression [%s]",
          name, actual, regex));
    }

    return ENABLED_BY_DEFAULT;
  }

  protected String getBuildConfigValue(String key) {
    try {
      return BuildConfigValueUtils.getAsString(key);
    } catch (IllegalAccessException ex) {
      return null;
    }
  }
}
