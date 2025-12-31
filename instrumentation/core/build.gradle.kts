plugins {
    alias(libs.plugins.android.junit)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "de.mannodermaus.junit5"

    defaultConfig {
        minSdk = Android.testCoreMinSdkVersion
        multiDexEnabled = true
    }
}

junitPlatform {
    filters {
        // See TaggedTests.kt for usage of this tag
        excludeTags("nope")
    }

    // Fail test execution when running on unsupported device
    // (TODO: Change this to the proper instrumentationTests API once released as stable)
    configurationParameter("de.mannodermaus.junit.unsupported.behavior", "fail")
}

// Use local project dependencies on android-test instrumentation libraries
// instead of relying on their Maven coordinates for this module
val instrumentationLibraryRegex = Regex("de\\.mannodermaus\\.junit5:android-test-(.+):")

configurations.all {
    if ("DebugAndroidTestRuntimeClasspath" in name) {
        resolutionStrategy.dependencySubstitution.all {
            instrumentationLibraryRegex.find(requested.toString())?.let { result ->
                useTarget(project(":${result.groupValues[1]}"))
            }
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.junit.jupiter.api)
    api(libs.androidx.test.core)
    // This is required by the "instrumentation-runner" companion library,
    // since it can't provide any JUnit 5 runtime libraries itself
    // due to fear of prematurely incrementing the minSdkVersion requirement.
    runtimeOnly(libs.junit.platform.launcher)
    runtimeOnly(libs.junit.platform.suiteapi)
    runtimeOnly(libs.junit.jupiter.engine)

    // This transitive dependency of JUnit 5 is required to be on the runtime classpath,
    // since otherwise ART will print noisy logs to console when trying to resolve any
    // of the annotations of JUnit 5 (see #291 for more info)
    runtimeOnly(libs.apiguardian)

    androidTestImplementation(libs.junit.jupiter.api)
    androidTestImplementation(libs.junit.jupiter.params)
    androidTestImplementation(libs.espresso)
    androidTestRuntimeOnly(project(":runner"))

    testImplementation(project(":testutil"))
}

project.configureDeployment(Artifacts.Instrumentation.Core)
