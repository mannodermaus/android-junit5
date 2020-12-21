buildscript {
  repositories {
    google()
    jcenter()
    jitpack()
  }
  dependencies {
    classpath(Plugins.kotlin)
    classpath(Plugins.bintray)
    classpath(Plugins.versions)
    classpath(Plugins.dokkaCore)
  }
}

subprojects {
  repositories {
    google()
    jcenter()
    sonatypeSnapshots()
  }

  // Configure publishing (if the project is eligible for publication)
  configureDeployConfig()
}

tasks.create<GenerateReadme>("generateReadme") {
  // Find folder containing README.md
  // (required because this script file is included through symlinks in sub-projects)
  var rootFolder = project.rootDir
  while (rootFolder != null && rootFolder.exists()) {
    val inFile = File(rootFolder, "README.md.template")
    val outFile = File(rootFolder, "README.md")

    if (inFile.exists() && outFile.exists()) {
      this.inputTemplateFile = inFile
      this.outputFile = outFile
      break
    }

    rootFolder = rootFolder.parentFile
  }
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
