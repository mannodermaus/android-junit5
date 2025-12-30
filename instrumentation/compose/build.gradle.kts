plugins {
    alias(libs.plugins.android.junit)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
}

android {
    namespace = "de.mannodermaus.junit5.compose"

    defaultConfig {
        minSdk = Android.testComposeMinSdkVersion

        testInstrumentationRunnerArguments["runnerBuilder"] =
            "de.mannodermaus.junit5.AndroidJUnitFrameworkBuilder"
    }

    buildFeatures { compose = true }
}

junitPlatform {
    // Using local dependency instead of Maven coordinates
    instrumentationTests.enabled = false
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines)

    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.vintage.api)
    implementation(libs.espresso)

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.uitooling)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    api(libs.compose.test.core)
    api(libs.compose.test.junit4)
    implementation(libs.compose.test.manifest)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.junit.jupiter.api)
    androidTestImplementation(libs.junit.jupiter.params)
    androidTestImplementation(libs.espresso)

    androidTestRuntimeOnly(project(":runner"))
    androidTestRuntimeOnly(libs.androidx.test.runner)
}

project.configureDeployment(Artifacts.Instrumentation.Compose)
