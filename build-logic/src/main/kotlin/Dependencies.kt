@file:Suppress("ClassName")

object libs {
    object versions {
        const val kotlin = "1.5.21"
        const val junitJupiter = "5.8.0-M1"
        const val junitVintage = "5.8.0-M1"
        const val junitPlatform = "1.8.0-M1"
        const val truth = "1.1.3"
        const val androidXTest = "1.4.0"
    }

    object plugins {
        val android = "com.android.tools.build:gradle:${SupportedAgp.values().first().version}"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin}"
        const val shadow = "com.github.jengelman.gradle.plugins:shadow:6.1.0"
        const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:1.5.0"
    }

    // Libraries
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions.kotlin}"
    const val javaSemver = "com.github.zafarkhaja:java-semver:0.9.0"

    const val junitJupiterApi = "org.junit.jupiter:junit-jupiter-api:${versions.junitJupiter}"
    const val junitJupiterParams = "org.junit.jupiter:junit-jupiter-params:${versions.junitJupiter}"
    const val junitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine:${versions.junitJupiter}"
    const val junitVintageEngine = "org.junit.vintage:junit-vintage-engine:${versions.junitVintage}"
    const val junitPlatformCommons = "org.junit.platform:junit-platform-commons:${versions.junitPlatform}"
    const val junitPlatformRunner = "org.junit.platform:junit-platform-runner:${versions.junitPlatform}"

    // Testing
    const val junit4 = "junit:junit:4.13.2"
    const val korte = "com.soywiz.korlibs.korte:korte:2.2.0"
    const val konfToml = "com.uchuhimo:konf-toml:1.1.2"
    const val mockitoCore = "org.mockito:mockito-core:3.11.1"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
    const val truth = "com.google.truth:truth:${versions.truth}"
    const val truthJava8Extensions = "com.google.truth.extensions:truth-java8-extension:${versions.truth}"

    const val androidXTestCore = "androidx.test:core:${versions.androidXTest}"
    const val androidXTestRunner = "androidx.test:runner:${versions.androidXTest}"
    const val androidXTestMonitor = "androidx.test:monitor:${versions.androidXTest}"
    const val espressoCore = "androidx.test.espresso:espresso-core:3.4.0"
}
