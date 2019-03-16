package de.mannodermaus.gradle.plugins.junit5.util

enum class FileLanguage2(
    val sourceDirectoryName: String,
    private val fileExtension: String) {
  Java("java", "java"),
  Kotlin("kotlin", "kt");

  fun appendExtension(nameWithoutExtension: String) =
      "$nameWithoutExtension.$fileExtension"
}
