apply(plugin = "io.codearte.nexus-staging")

buildscript {
  repositories {
    google()
    mavenCentral()
    jcenter()
    jitpack()
    gradlePluginPortal()
  }
  dependencies {
    classpath(Plugins.kotlin)
    classpath(Plugins.android.dependency)
    classpath(Plugins.versions)
    classpath(Plugins.dokkaCore)
    classpath(Plugins.nexusStaging)
    classpath(Plugins.nexusPublishing)
  }
}

subprojects {
  repositories {
    google()
    mavenCentral()
    jcenter()
    sonatypeSnapshots()
  }

  // Configure publishing (if the project is eligible for publication)
  configureDeployConfig()
}

fun Project.configureDeployConfig() {
  // ------------------------------------------------------------------------------------------------
  // Deployment Setup
  //
  // Releases are pushed to JCenter via Bintray, while snapshots are pushed to Sonatype OSS.
  // This section defines the necessary tasks to push new releases and snapshots using Gradle tasks.
  // ------------------------------------------------------------------------------------------------
  val configuration = Artifacts.from(this) ?: return
  ext["deployConfig"] = configuration
  ext["deployCredentials"] = DeployedCredentials(this)
}
