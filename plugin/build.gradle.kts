plugins {
    id("io.github.gradle-nexus.publish-plugin").version("2.0.0")
    id("org.jetbrains.kotlinx.binary-compatibility-validator").version("0.14.0")
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(libs.plugins.kotlin)
        classpath(libs.plugins.dokka)
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
}

apiValidation {
    ignoredPackages.add("de.mannodermaus.gradle.plugins.junit5.internal")
}

tasks.create<GenerateReadme>("generateReadme") {
    // Find folder containing README.md
    // (required because this script file is included through symlinks in subprojects)
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
