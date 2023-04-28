@file:Suppress("ClassName")

object libs {
    object versions {
        const val kotlin = "1.7.0"
        const val junitJupiter = "5.9.0"
        const val junitVintage = "5.9.0"
        const val junitPlatform = "1.9.0"
        const val truth = "1.1.3"
        const val androidXTest = "1.4.0"
        const val compose = "1.2.0"
    }

    object plugins {
        fun android(version: SupportedAgp) = "com.android.tools.build:gradle:${version.version}"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin}"
        const val shadow = "com.github.jengelman.gradle.plugins:shadow:6.1.0"
        const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:1.7.0"
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
    const val kotlinCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4"
    const val javaSemver = "com.github.zafarkhaja:java-semver:0.9.0"

    const val junitJupiterApi = "org.junit.jupiter:junit-jupiter-api:${versions.junitJupiter}"
    const val junitJupiterParams = "org.junit.jupiter:junit-jupiter-params:${versions.junitJupiter}"
    const val junitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine:${versions.junitJupiter}"
    const val junitVintageEngine = "org.junit.vintage:junit-vintage-engine:${versions.junitVintage}"
    const val junitPlatformCommons = "org.junit.platform:junit-platform-commons:${versions.junitPlatform}"
    const val junitPlatformRunner = "org.junit.platform:junit-platform-runner:${versions.junitPlatform}"

    const val composeUi = "androidx.compose.ui:ui:${versions.compose}"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling:${versions.compose}"
    const val composeFoundation = "androidx.compose.foundation:foundation:${versions.compose}"
    const val composeMaterial = "androidx.compose.material:material:${versions.compose}"

    // Testing
    const val junit4 = "junit:junit:4.13.2"
    const val korte = "com.soywiz.korlibs.korte:korte:2.2.0"
    const val konfToml = "com.uchuhimo:konf-toml:1.1.2"
    const val mockitoCore = "org.mockito:mockito-core:3.11.1"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
    const val truth = "com.google.truth:truth:${versions.truth}"
    const val truthJava8Extensions = "com.google.truth.extensions:truth-java8-extension:${versions.truth}"
    const val robolectric = "org.robolectric:robolectric:4.8.1"

    const val androidXTestCore = "androidx.test:core:${versions.androidXTest}"
    const val androidXTestRunner = "androidx.test:runner:${versions.androidXTest}"
    const val androidXTestMonitor = "androidx.test:monitor:${versions.androidXTest}"
    const val espressoCore = "androidx.test.espresso:espresso-core:3.4.0"

    const val composeUiTest = "androidx.compose.ui:ui-test:${versions.compose}"
    const val composeUiTestJUnit4 = "androidx.compose.ui:ui-test-junit4:${versions.compose}"
    const val composeUiTestManifest = "androidx.compose.ui:ui-test-manifest:${versions.compose}"
}
