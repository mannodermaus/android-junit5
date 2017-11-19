package de.mannodermaus.gradle.plugins.junit5.providers

import java.io.File

interface TestRootDirectoryProvider {
  fun testRootDirectories(): Set<File>
}
