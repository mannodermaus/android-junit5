package de.mannodermaus.gradle.plugins.junit5.util

enum class AgpVersion(val fileKey: String) {
  AGP_32X("agp32x"),
  AGP_33X("agp33x"),
  AGP_34X("agp34x"),
  AGP_35X("agp35x"),
  AGP_36X("agp36x"),
  AGP_40X("agp40x");

  companion object {
    fun latest() = AGP_34X
  }
}
