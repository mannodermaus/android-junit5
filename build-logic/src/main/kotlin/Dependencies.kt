@file:Suppress("ClassName")

object libs {
    object versions {
        const val kotlin = "2.3.0"
        const val junitJupiter = "5.14.0"
        const val junitVintage = "5.14.0"
        const val junitPlatform = "1.14.0"

        const val compose = "1.10.0"
        const val androidXMultidex = "2.0.1"
        const val androidXTestAnnotation = "1.0.1"
        const val androidXTestCore = "1.6.1"
        const val androidXTestMonitor = "1.7.2"
        const val androidXTestRunner = "1.6.2"

        const val activityCompose = "1.10.1"
        const val apiGuardian = "1.1.2"
        const val coroutines = "1.10.2"
        const val dokka = "2.0.0"
        const val espresso = "3.6.1"
        const val junit4 = "4.13.2"
        const val konfToml = "1.1.2"
        const val kotlinxBinaryCompatibilityValidator = "0.17.0"
        const val nexusPublish = "2.0.0"
        const val korte = "2.4.12"
        const val mockitoCore = "5.16.0"
        const val mockitoKotlin = "5.4.0"
        const val robolectric = "4.14.1"
        const val shadow = "8.1.1"
        const val truth = "1.4.4"
    }

    object plugins {
        fun android(version: SupportedAgp) = "com.android.tools.build:gradle:${version.version}"
        const val composeCompiler = "org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:${versions.kotlin}"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin}"
        const val shadow = "com.github.johnrengelman:shadow:${libs.versions.shadow}"
        const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${libs.versions.dokka}"
    }

    // Libraries
    val androidTools = run {
        // The version of this library is linked to AGP
        // (essentially: "AGP + 23.0.0")
        val agpVersionParts = SupportedAgp.oldest.version.split('.')
        val toolsVersion = "${23 + agpVersionParts.first().toInt()}." + agpVersionParts.drop(1).joinToString(".")
        "com.android.tools:common:$toolsVersion"
    }

    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions.kotlin}"
    const val kotlinCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}"

    const val junitJupiterApi = "org.junit.jupiter:junit-jupiter-api:${versions.junitJupiter}"
    const val junitJupiterParams = "org.junit.jupiter:junit-jupiter-params:${versions.junitJupiter}"
    const val junitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine:${versions.junitJupiter}"
    const val junitVintageEngine = "org.junit.vintage:junit-vintage-engine:${versions.junitVintage}"
    const val junitPlatformCommons = "org.junit.platform:junit-platform-commons:${versions.junitPlatform}"
    const val junitPlatformLauncher = "org.junit.platform:junit-platform-launcher:${versions.junitPlatform}"
    const val junitPlatformRunner = "org.junit.platform:junit-platform-runner:${versions.junitPlatform}"
    const val apiguardianApi = "org.apiguardian:apiguardian-api:${versions.apiGuardian}"

    const val composeUi = "androidx.compose.ui:ui:${versions.compose}"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling:${versions.compose}"
    const val composeFoundation = "androidx.compose.foundation:foundation:${versions.compose}"
    const val composeMaterial = "androidx.compose.material:material:${versions.compose}"
    const val composeActivity = "androidx.activity:activity-compose:${versions.activityCompose}"

    // Testing
    const val junit4 = "junit:junit:${versions.junit4}"
    const val korte = "com.soywiz.korlibs.korte:korte:${versions.korte}"
    const val konfToml = "com.uchuhimo:konf-toml:${versions.konfToml}"
    const val mockitoCore = "org.mockito:mockito-core:${versions.mockitoCore}"
    const val mockitoKotlin = "org.mockito.kotlin:mockito-kotlin:${versions.mockitoKotlin}"
    const val truth = "com.google.truth:truth:${versions.truth}"
    const val truthJava8Extensions = "com.google.truth.extensions:truth-java8-extension:${versions.truth}"
    const val robolectric = "org.robolectric:robolectric:${versions.robolectric}"

    const val androidXMultidex = "androidx.multidex:multidex:${versions.androidXMultidex}"
    const val androidXTestAnnotation = "androidx.test:annotation:${versions.androidXTestAnnotation}"
    const val androidXTestCore = "androidx.test:core:${versions.androidXTestCore}"
    const val androidXTestMonitor = "androidx.test:monitor:${versions.androidXTestMonitor}"
    const val androidXTestRunner = "androidx.test:runner:${versions.androidXTestRunner}"
    const val espressoCore = "androidx.test.espresso:espresso-core:${versions.espresso}"

    const val composeUiTest = "androidx.compose.ui:ui-test:${versions.compose}"
    const val composeUiTestJUnit4 = "androidx.compose.ui:ui-test-junit4:${versions.compose}"
    const val composeUiTestManifest = "androidx.compose.ui:ui-test-manifest:${versions.compose}"

    // Documentation
    // For the latest version refer to GitHub repo neboskreb/instant-task-executor-extension
    const val instantTaskExecutorExtension = "io.github.neboskreb:instant-task-executor-extension:1.0.0"
}
