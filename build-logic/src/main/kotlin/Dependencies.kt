@file:Suppress("ClassName")

object libs {
    object versions {
        const val kotlin = "1.3.72"
        const val junitJupiter = "5.7.2"
        const val junitPlatform = "1.7.2"
        const val truth = "0.43"
        const val androidXTest = "1.2.0"
    }

    object plugins {
        val android = "com.android.tools.build:gradle:${SupportedAgp.values().first().version}"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin}"
        const val versions = "com.github.ben-manes:gradle-versions-plugin:0.39.0"
        const val shadow = "com.github.jengelman.gradle.plugins:shadow:6.1.0"
        const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:1.4.20"
        const val nexusStaging = "io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.22.0"
        const val nexusPublishing = "de.marcphilipp.gradle:nexus-publish-plugin:0.4.0"
    }

    // Libraries
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions.kotlin}"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${versions.kotlin}"
    const val javaSemver = "com.github.zafarkhaja:java-semver:0.9.0"

    const val junitJupiterApi = "org.junit.jupiter:junit-jupiter-api:${versions.junitJupiter}"
    const val junitJupiterParams = "org.junit.jupiter:junit-jupiter-params:${versions.junitJupiter}"
    const val junitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine:${versions.junitJupiter}"
    const val junitPlatformCommons = "org.junit.platform:junit-platform-commons:${versions.junitPlatform}"
    const val junitPlatformRunner = "org.junit.platform:junit-platform-runner:${versions.junitPlatform}"

    // Testing
    const val junit4 = "junit:junit:4.13"
    const val korte = "com.soywiz.korlibs.korte:korte:1.10.15" // TODO After raise to 2.x, remove jcenter()
    const val konfToml = "com.uchuhimo:konf-toml:0.22.1"
    const val mockitoCore = "org.mockito:mockito-core:2.19.0"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0"
    const val truth = "com.google.truth:truth:${versions.truth}"
    const val truthJava8Extensions = "com.google.truth.extensions:truth-java8-extension:${versions.truth}"

    const val androidXTestCore = "androidx.test:core:${versions.androidXTest}"
    const val androidXTestRunner = "androidx.test:runner:${versions.androidXTest}"
    const val androidXTestMonitor = "androidx.test:monitor:${versions.androidXTest}"
    const val espressoCore = "androidx.test.espresso:espresso-core:3.2.0"
}
