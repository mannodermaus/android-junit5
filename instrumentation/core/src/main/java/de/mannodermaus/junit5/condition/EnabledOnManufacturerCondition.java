package de.mannodermaus.junit5.condition;

import android.annotation.TargetApi;
import android.os.Build;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

class EnabledOnManufacturerCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_BY_DEFAULT =
      ConditionEvaluationResult.enabled("@EnabledOnManufacturer is not present");

  static ConditionEvaluationResult enabled() {
    return ConditionEvaluationResult.enabled("Enabled on Manufacturer: " + Build.MANUFACTURER);
  }

  static ConditionEvaluationResult disabled() {
    return ConditionEvaluationResult.disabled("Disabled on Manufacturer: " + Build.MANUFACTURER);
  }

  @TargetApi(24)
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<EnabledOnManufacturer> optional = findAnnotation(context.getElement(), EnabledOnManufacturer.class);

    if (optional.isPresent()) {
      EnabledOnManufacturer annotation = optional.get();
      String[] patterns = annotation.value();
      boolean ignoreCase = annotation.ignoreCase();
      Preconditions.condition(patterns.length > 0, "You must declare at least one Manufacturer in @EnabledOnManufacturer");

      return Arrays.stream(patterns).anyMatch(value -> matchesCurrentManufacturer(value, ignoreCase))
          ? enabled()
          : disabled();
    }

    return ENABLED_BY_DEFAULT;
  }

  private boolean matchesCurrentManufacturer(String value, boolean ignoreCase) {
    if (ignoreCase) {
      return Build.MANUFACTURER.equalsIgnoreCase(value);
    } else {
      return Build.MANUFACTURER.equals(value);
    }
  }
}
