package de.mannodermaus.gradle.plugins.junit5.util

enum FileLanguage {
  Java("java", "java"),
  Kotlin("kotlin", "kt");

  final String sourceDirectoryName
  private final String fileExtension

  FileLanguage(String sourceDirectoryName, String fileExtension) {
    this.sourceDirectoryName = sourceDirectoryName
    this.fileExtension = fileExtension
  }

  String appendExtension(String nameWithoutExtension) {
    return "${nameWithoutExtension}.$fileExtension"
  }
}
