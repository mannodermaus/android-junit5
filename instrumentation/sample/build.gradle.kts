plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.junit)
    alias(libs.plugins.kotlin.android)
    id("jacoco")
}

android {
    namespace = "de.mannodermaus.junit5.sample"

    defaultConfig {
        applicationId = "de.mannodermaus.junit5.sample"
        minSdk = Android.sampleMinSdkVersion
        targetSdk = Android.targetSdkVersion
        versionCode = 1
        versionName = "1.0"

        // Make sure to use the AndroidJUnitRunner (or a sub-class) in order to hook in the JUnit 5
        // Test Builder
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // These two lines are not needed for a normal integration;
        // this sample project disables the automatic integration, so it must be done manually
        testInstrumentationRunnerArguments["runnerBuilder"] =
            "de.mannodermaus.junit5.AndroidJUnitFrameworkBuilder"
        testInstrumentationRunnerArguments["configurationParameters"] =
            "junit.jupiter.execution.parallel.enabled=true,junit.jupiter.execution.parallel.mode.default=concurrent"

        buildFeatures { buildConfig = true }

        buildConfigField("boolean", "MY_VALUE", "true")

        testOptions { animationsDisabled = true }
    }
}

junitPlatform {
    // Configure JUnit 5 tests here
    filters("debug") { excludeTags("slow") }
}

replaceAndroidTestLibsWithLocalProjectDependencies()

dependencies {
    implementation(libs.kotlin.stdlib)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.junit.vintage.api)
    androidTestImplementation(libs.androidx.test.runner)

    // Android Instrumentation Tests wth JUnit 5
    androidTestImplementation(libs.junit.jupiter.api)
    androidTestImplementation(libs.junit.jupiter.params)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(project(":core"))
    androidTestRuntimeOnly(project(":runner"))
}
