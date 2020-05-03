buildscript {
  repositories {
    google()
    jcenter()
    jitpack()
  }
  dependencies {
    classpath(Plugins.kotlin)
    classpath(Plugins.android)
    classpath(Plugins.androidMavenPublish)
    classpath(Plugins.bintray)
    classpath(Plugins.androidMavenGradle)
    classpath(Plugins.versions)
    classpath(Plugins.dokkaCore)
    classpath(Plugins.dokkaAndroid)
  }
}

allprojects {
  repositories {
    google()
    jcenter()
    sonatypeSnapshots()
  }

  // Store deployment credentials (used in deployment.gradle)
  extra["deployCredentials"] = DeployCredentials(project)
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
