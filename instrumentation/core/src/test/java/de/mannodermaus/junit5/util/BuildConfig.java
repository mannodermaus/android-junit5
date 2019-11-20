package de.mannodermaus.junit5.util;

// Used reflectively by the integration tests for BuildConfig-related conditions
@SuppressWarnings("unused")
public final class BuildConfig {
  public static final boolean DEBUG = Boolean.parseBoolean("true");
  public static final String VERSION_NAME = "1.0";
}
