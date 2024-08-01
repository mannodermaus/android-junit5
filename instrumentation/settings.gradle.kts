@file:Suppress("UnstableApiUsage")

rootProject.name = "android-junit5-instrumentation"

includeBuild("../build-logic")
include(":core")
include(":compose")
include(":extensions")
include(":runner")
include(":sample")
include(":testutil")
include(":testutil-reflect")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            setUrl("https://jitpack.io")
        }
        maven {
            setUrl("https://oss.sonatype.org/content/repositories/snapshots")
            mavenContent { snapshotsOnly() }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://oss.sonatype.org/content/repositories/snapshots")
            mavenContent { snapshotsOnly() }
        }
    }
}
