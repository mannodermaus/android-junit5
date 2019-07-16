package de.mannodermaus.junit5.condition;

import android.annotation.TargetApi;
import android.os.Build;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;

import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

class EnabledOnApiCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_BY_DEFAULT =
      ConditionEvaluationResult.enabled("@EnabledOnApi is not present");

  static final int NOT_SET = -1;

  static ConditionEvaluationResult enabled() {
    return ConditionEvaluationResult.enabled("Enabled on API " + Build.VERSION.SDK_INT);
  }

  static ConditionEvaluationResult disabled() {
    return ConditionEvaluationResult.disabled("Disabled on API " + Build.VERSION.SDK_INT);
  }

  @TargetApi(24)
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<EnabledOnApi> optional = findAnnotation(context.getElement(), EnabledOnApi.class);

    if (optional.isPresent()) {
      EnabledOnApi annotation = optional.get();
      int minApi = annotation.min();
      int maxApi = annotation.max();

      boolean hasLowerBound = minApi != NOT_SET;
      boolean hasUpperBound = maxApi != NOT_SET;
      Preconditions.condition(hasLowerBound || hasUpperBound, "At least one value must be provided in @EnabledOnApi");

      // Constrain the current API Level based on the presence of "minApi" & "maxApi":
      // If either one is not set at all, that part of the conditional becomes true automatically
      return (!hasLowerBound || Build.VERSION.SDK_INT >= minApi) && (!hasUpperBound || Build.VERSION.SDK_INT <= maxApi)
          ? enabled()
          : disabled();
    }

    return ENABLED_BY_DEFAULT;
  }
}
