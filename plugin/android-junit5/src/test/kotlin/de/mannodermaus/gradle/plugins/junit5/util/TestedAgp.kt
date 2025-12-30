package de.mannodermaus.gradle.plugins.junit5.util

data class TestedAgp(
    val shortVersion: String,
    val version: String,
    val requiresGradle: String,
    val requiresCompileSdk: Int?,
) {
    val fileKey: String = "agp${shortVersion.replace(".", "")}x"
}
