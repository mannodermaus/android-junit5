@file:Suppress("UnstableApiUsage")

rootProject.name = "android-junit5-plugin"

includeBuild("../build-logic")
include(":android-junit5")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            setUrl("https://jitpack.io")
        }
        maven {
            setUrl("https://central.sonatype.com/repository/maven-snapshots")
            mavenContent { snapshotsOnly() }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://central.sonatype.com/repository/maven-snapshotss")
            mavenContent { snapshotsOnly() }
        }
    }
    
    versionCatalogs {
        create("libs") {
            from(files("../build-logic/gradle/libs.versions.toml"))
        }
    }
}
