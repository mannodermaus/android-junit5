import de.mannodermaus.gradle.plugins.junit5.Deps
import java.util.Properties

buildscript {
  rootProject.apply { from(rootProject.file("gradle/dependencies.gradle.kts")) }
  repositories {
    google()
    jcenter()
    maven("https://jitpack.io")
  }
  dependencies {
    classpath(kotlin("gradle-plugin", extra["versions.kotlin"] as String))
    classpath(extra["plugins.android"] as String)
    classpath(extra["plugins.androidMaven"] as String)
    classpath(extra["plugins.bintray"] as String)
    classpath(extra["plugins.dcendentsMaven"] as String)
    classpath(extra["plugins.versions"] as String)
  }
}

// Populate deployment credentials in an environment-aware fashion.
//
// * Local development:
//      Stored in local.properties file on the machine
// * CI Server:
//      Stored in environment variables before launch
val properties = Properties().apply {
  val credentialsFile = File(project.rootDir, "local.properties")
  if (credentialsFile.exists()) {
    load(credentialsFile.inputStream())
  }
}

internal val bintrayUser = properties.getProperty("BINTRAY_USER", System.getenv("bintrayUser"))
internal val bintrayKey = properties.getProperty("BINTRAY_KEY", System.getenv("bintrayKey"))
internal val sonatypeUser = properties.getProperty("SONATYPE_USER", System.getenv("sonatypeUser"))
internal val sonatypePass = properties.getProperty("SONATYPE_PASS", System.getenv("sonatypePass"))

allprojects {
  repositories {
    google()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }

  // Store deployment credentials
  extra["deployment.bintrayUser"] = bintrayUser
  extra["deployment.bintrayKey"] = bintrayKey
  extra["deployment.sonatypeUser"] = sonatypeUser
  extra["deployment.sonatypePass"] = sonatypePass

  apply(from = "$rootDir/gradle/dependencies.gradle.kts")
}
