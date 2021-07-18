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
        classpath(libs.plugins.versions)
        classpath(libs.plugins.dokka)
        classpath(libs.plugins.nexusStaging)
        classpath(libs.plugins.nexusPublishing)
        classpath(libs.plugins.shadow)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        sonatypeSnapshots()
    }

    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(libs.versions.kotlin)
            }
        }
    }

    // Configure publishing (if the project is eligible for publication)
    configureDeployConfig()
}

tasks.create<GenerateReadme>("generateReadme") {
    // Find folder containing README.md
    // (required because this script file is included through symlinks in sub-projects)
    var rootFolder: File? = project.rootDir
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
    // Releases and snapshots are pushed to Maven Central.
    // This section defines the necessary tasks to push new releases and snapshots using Gradle tasks.
    // ------------------------------------------------------------------------------------------------
    val configuration = Artifacts.from(this) ?: return
    ext["deployConfig"] = configuration
    ext["deployCredentials"] = DeployedCredentials(this)
}
