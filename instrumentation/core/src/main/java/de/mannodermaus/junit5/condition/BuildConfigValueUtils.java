package de.mannodermaus.junit5.condition;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("NewApi")
final class BuildConfigValueUtils {

  private static Class BUILD_CONFIG_CLASS = null;
  private static final Map<String, Field> BUILD_CONFIG_FIELD_CACHE = new HashMap<>();

  private BuildConfigValueUtils() {
    //no instance
  }

  /**
   * Reflectively look up a BuildConfig field's value.
   * This caches previous lookups to maximize performance.
   * @param key Key of the entry to obtain
   * @return The value of this entry, if any
   */
  @Nullable
  static synchronized String getAsString(String key) throws IllegalAccessException {
    Class buildConfigClass = BUILD_CONFIG_CLASS;
    if (buildConfigClass == null) {
      buildConfigClass = findBuildConfigClass();
      BUILD_CONFIG_CLASS = buildConfigClass;
    }
    if (buildConfigClass == null) {
      throw new IllegalAccessException("BuildConfig not found");
    }

    Field buildConfigField = findBuildConfigField(buildConfigClass, key);
    if (buildConfigField == null) {
      throw new IllegalAccessException("BuildConfig field '" + key + "' not found");
    }

    return getBuildConfigValue(buildConfigField);
  }

  /* Private */

  private static Class findBuildConfigClass() {
    try {
      Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
      Context targetContext = instrumentation.getTargetContext();
      String packageName = targetContext.getPackageName();
      String buildConfigClassName = packageName + ".BuildConfig";
      return Class.forName(buildConfigClassName);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Field findBuildConfigField(Class clazz, String fieldName) {
    try {
      Field field = BUILD_CONFIG_FIELD_CACHE.get(fieldName);
      if (field == null) {
        field = clazz.getField(fieldName);
        field.setAccessible(true);
        BUILD_CONFIG_FIELD_CACHE.put(fieldName, field);
      }
      return field;
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static String getBuildConfigValue(Field field) throws IllegalAccessException {
    try {
      Object value = field.get(null);
      if (value != null) {
        return String.valueOf(value);
      } else {
        return null;
      }
    } catch (Throwable ignored) {
      throw new IllegalAccessException("BuildConfig field value cannot be retrieved");
    }
  }
}
