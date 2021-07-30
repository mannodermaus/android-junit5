plugins {
    id("io.github.gradle-nexus.publish-plugin").version("1.1.0")
    id("com.github.ben-manes.versions").version("0.39.0")
    id("org.jetbrains.kotlinx.binary-compatibility-validator").version("0.6.0")
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jitpack()
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
