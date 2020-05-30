package de.mannodermaus.gradle.plugins.junit5.util

data class AgpUnderTest(val shortVersion: String, val version: String, val requiresGradle: String?) {
  val fileKey: String = "agp${shortVersion.replace(".", "")}x"
}
