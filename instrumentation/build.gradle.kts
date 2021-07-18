apply(plugin = "io.codearte.nexus-staging")
apply(plugin = "com.github.ben-manes.versions")

buildscript {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    jitpack()
  }
  dependencies {
    classpath(libs.plugins.kotlin)
    classpath(libs.plugins.android)
    classpath(libs.plugins.versions)
    classpath(libs.plugins.dokka)
    classpath(libs.plugins.nexusStaging)
    classpath(libs.plugins.nexusPublishing)
  }
}

subprojects {
  repositories {
    google()
    mavenCentral()
    sonatypeSnapshots()
  }

  // Configure publishing (if the project is eligible for publication)
  configureDeployConfig()
}

fun Project.configureDeployConfig() {
  // ------------------------------------------------------------------------------------------------
  // Deployment Setup
  //
  // Releases are pushed to Maven Central, while snapshots are pushed to Sonatype OSS.
  // This section defines the necessary tasks to push new releases and snapshots using Gradle tasks.
  // ------------------------------------------------------------------------------------------------
  val configuration = Artifacts.from(this) ?: return
  ext["deployConfig"] = configuration
  ext["deployCredentials"] = DeployedCredentials(this)
}
