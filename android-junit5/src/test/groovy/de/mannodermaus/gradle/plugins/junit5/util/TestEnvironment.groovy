package de.mannodermaus.gradle.plugins.junit5.util

import com.android.annotations.Nullable

import static StringUtils.splitClasspath

/*
 * Encapsulates environment properties related to running
 * Unit Tests that interface with an Android SDK installation.
 *
 * Typically, test cases don't need to instantiate this themselves,
 * as its creation is hooked into the lifecycle of the enclosing test class.
 */

class TestEnvironment {

  private static final ANDROID_SDK_FILE_NAME = "local.properties"
  private static final ANDROID_SDK_PROP_NAME = "sdk.dir"
  private static final ANDROID_HOME_ENVVAR_NAME = "ANDROID_HOME"

  private static
  final ENVIRONMENT_RESOURCE_NAME = "/de/mannodermaus/gradle/plugins/android_junit5/testenv.properties"
  private static final COMPILE_SDK_PROP_NAME = "compileSdkVersion"
  private static final BUILD_TOOLS_PROP_NAME = "buildToolsVersion"
  private static final MIN_SDK_PROP_NAME = "minSdkVersion"
  private static final TARGET_SDK_PROP_NAME = "targetSdkVersion"

  // Location of the Android SDK pointed to by the sdk.dir property
  final File androidSdkFolder

  // Android Build Environment
  final String compileSdkVersion
  final String buildToolsVersion
  final int minSdkVersion
  final int targetSdkVersion

  // Plugin Classpath for Unit Tests based on Gradle TestKit
  final List<File> pluginClasspathFiles
  @Lazy String pluginClasspathString = {
    splitClasspath(pluginClasspathFiles)
  }()

  TestEnvironment() {
    androidSdkFolder = loadAndroidSdkFolder()

    def envProps = loadAndroidEnvironment()
    compileSdkVersion = envProps.getProperty(COMPILE_SDK_PROP_NAME)
    buildToolsVersion = envProps.getProperty(BUILD_TOOLS_PROP_NAME)
    minSdkVersion = envProps.getProperty(MIN_SDK_PROP_NAME).toInteger()
    targetSdkVersion = envProps.getProperty(TARGET_SDK_PROP_NAME).toInteger()

    pluginClasspathFiles = loadPluginClasspath()
  }

  private File loadAndroidSdkFolder() {
    // Try local project first, fall back to Environment Variable, throw if nothing works
    File folder = loadAndroidSdkFromProject()

    if (folder == null) {
      folder = loadAndroidSdkFromEnvVar()
    }

    if (folder == null) {
      throw new AssertionError(
          "Android SDK couldn't be found. Either local.properties file in project root is missing, " +
              "it doesn't include the required 'sdk.dir' statement, " +
              "or there is no ANDROID_HOME environment variable!")
    }

    return folder
  }

  @Nullable
  private File loadAndroidSdkFromProject() {
    File rootFile = new File(System.getProperty("user.dir")).parentFile
    File localPropsFile = new File(rootFile, ANDROID_SDK_FILE_NAME)
    if (localPropsFile.exists()) {
      def sdkFolderProp = localPropsFile.readLines()
          .find { it.startsWith(ANDROID_SDK_PROP_NAME) }
      sdkFolderProp = sdkFolderProp.substring(sdkFolderProp.indexOf('=') + 1).trim()

      return new File(sdkFolderProp)
    }

    return null
  }

  @Nullable
  private File loadAndroidSdkFromEnvVar() {
    String envvar = System.getenv(ANDROID_HOME_ENVVAR_NAME)
    if (envvar) {
      return new File(envvar)
    }

    return null
  }

  private Properties loadAndroidEnvironment() {
    Properties envProps = new Properties()
    getClass().getResourceAsStream(ENVIRONMENT_RESOURCE_NAME)
        .withReader { envProps.load(it) }

    return envProps
  }

  private List<File> loadPluginClasspath() {
    def pluginClasspathResource = getClass().getResource("/plugin-classpath.txt")
    if (pluginClasspathResource == null) {
      throw new IllegalStateException(
          "Did not find plugin classpath resource, run `testClasses` build task.")
    }

    return pluginClasspathResource.readLines().collect { new File(it) }
  }
}
