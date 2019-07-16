package de.mannodermaus.junit5.condition;

import android.annotation.TargetApi;
import android.os.Build;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;

import java.util.Arrays;
import java.util.Optional;

import static de.mannodermaus.junit5.condition.EnabledOnManufacturerCondition.disabled;
import static de.mannodermaus.junit5.condition.EnabledOnManufacturerCondition.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

class DisabledOnManufacturerCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_BY_DEFAULT =
      ConditionEvaluationResult.enabled("@DisabledOnManufacturer is not present");

  @TargetApi(24)
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<DisabledOnManufacturer> optional = findAnnotation(context.getElement(), DisabledOnManufacturer.class);

    if (optional.isPresent()) {
      DisabledOnManufacturer annotation = optional.get();
      String[] patterns = annotation.value();
      boolean ignoreCase = annotation.ignoreCase();
      Preconditions.condition(patterns.length > 0, "You must declare at least one Manufacturer in @DisabledOnManufacturer");

      return Arrays.stream(patterns).anyMatch(value -> matchesCurrentManufacturer(value, ignoreCase))
          ? disabled()
          : enabled();
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
