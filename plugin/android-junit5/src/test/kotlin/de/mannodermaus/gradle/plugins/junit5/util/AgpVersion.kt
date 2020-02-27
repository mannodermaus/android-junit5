package de.mannodermaus.gradle.plugins.junit5.util

enum class AgpVersion(val fileKey: String,
                      val requiresGradle: String? = null) {

  AGP_35X("agp35x"),
  AGP_36X("agp36x"),
  AGP_40X("agp40x");

  // Create a pretty string from the fileKey property.
  // Example:
  // fileKey    = "agp32x"
  // prettyName = "3.2"
  val prettyName: String = fileKey.substring(3, 5)
      .let { v -> "${v[0]}.${v[1]}" }

  companion object {
    fun latest() = AGP_36X
  }
}
