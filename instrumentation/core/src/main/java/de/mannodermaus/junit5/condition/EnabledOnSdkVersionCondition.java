package de.mannodermaus.junit5.condition;

import android.annotation.TargetApi;
import android.os.Build;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;

import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

class EnabledOnSdkVersionCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_BY_DEFAULT =
      ConditionEvaluationResult.enabled("@EnabledOnSdkVersion is not present");

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
    Optional<EnabledOnSdkVersion> optional = findAnnotation(context.getElement(), EnabledOnSdkVersion.class);

    if (optional.isPresent()) {
      EnabledOnSdkVersion annotation = optional.get();
      int fromApi = annotation.from();
      int untilApi = annotation.until();

      boolean hasLowerBound = fromApi != NOT_SET;
      boolean hasUpperBound = untilApi != NOT_SET;
      Preconditions.condition(hasLowerBound || hasUpperBound, "At least one value must be provided in @EnabledOnSdkVersion");

      // Constrain the current API Level based on the presence of "fromApi" & "untilApi":
      // If either one is not set at all, that part of the conditional becomes true automatically
      return (!hasLowerBound || Build.VERSION.SDK_INT >= fromApi) && (!hasUpperBound || Build.VERSION.SDK_INT <= untilApi)
          ? enabled()
          : disabled();
    }

    return ENABLED_BY_DEFAULT;
  }
}
