plugins {
  id("io.github.gradle-nexus.publish-plugin").version("2.0.0")
  id("org.jetbrains.kotlinx.binary-compatibility-validator").version("0.14.0")
}

buildscript {
  dependencies {
    classpath(libs.plugins.kotlin)
    classpath(libs.plugins.dokka)
    classpath(libs.plugins.android(SupportedAgp.newestStable))
  }
}

apiValidation {
  ignoredPackages.add("de.mannodermaus.junit5.internal")
  ignoredPackages.add("de.mannodermaus.junit5.compose.internal")
  ignoredProjects.add("sample")
  ignoredProjects.add("testutil")
  ignoredProjects.add("testutil-reflect")
}
