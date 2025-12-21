plugins {
    alias(libs.plugins.android.junit)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "de.mannodermaus.junit5.extensions"

    defaultConfig {
        minSdk = Android.testRunnerMinSdkVersion
    }
}

dependencies {
    implementation(libs.androidx.test.annotation)
    implementation(libs.androidx.test.runner)
    implementation(libs.junit.jupiter.api)

    testImplementation(project(":testutil"))
    testRuntimeOnly(libs.junit.jupiter.engine)
}

project.configureDeployment(Artifacts.Instrumentation.Extensions)
