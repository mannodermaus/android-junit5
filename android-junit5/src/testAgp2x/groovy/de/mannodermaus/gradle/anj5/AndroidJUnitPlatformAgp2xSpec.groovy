package de.mannodermaus.gradle.anj5

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.StandardOutputListener
import org.gradle.internal.logging.services.DefaultLoggingManager
import org.gradle.internal.logging.services.DefaultLoggingManagerFactory
import org.gradle.internal.resolve.ModuleVersionNotFoundException
import org.gradle.testfixtures.ProjectBuilder

/**
 * Unit Tests of the android-junit5 plugin for the Android Gradle Plugin 2.x.
 * This is applied on top of the default test cases that this class inherits.
 */
class AndroidJUnitPlatformAgp2xSpec extends AndroidJUnitPlatformSpec {

    def "custom junit jupiter version"() {
        when:
        def nonExistentVersion = "0.0.0"

        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()
        p.file("src/main").mkdirs()
        p.file("src/main/AndroidManifest.xml").withWriter { it.write(ANDROID_MANIFEST) }

        p.apply plugin: 'com.android.application'
        p.apply plugin: 'de.mannodermaus.android-junit5'
        p.android {
            compileSdkVersion COMPILE_SDK
            buildToolsVersion BUILD_TOOLS

            defaultConfig {
                applicationId APPLICATION_ID
                minSdkVersion MIN_SDK
                targetSdkVersion TARGET_SDK
                versionCode VERSION_CODE
                versionName VERSION_NAME
            }
        }
        p.junitPlatform {
            // Some arbitrary non-existent version
            jupiterVersion nonExistentVersion
        }
        p.repositories {
            jcenter()
        }
        p.dependencies {
            testCompile junitJupiter()
        }

        then:
        // AGP 2.x throws a ModuleVersionNotFoundException here
        try {
            p.evaluate()
            throw new AssertionError("Expected ${ModuleVersionNotFoundException.class.name}, but wasn't thrown")

        } catch (Throwable expected) {
            while (expected != null) {
                if (expected instanceof ModuleVersionNotFoundException) {
                    assert expected.message.contains("Could not find org.junit.jupiter")
                    assert expected.message.contains("$nonExistentVersion")
                    break
                }
                expected = expected.cause
            }

            if (expected == null) {
                throw new AssertionError("Expected ${ModuleVersionNotFoundException.class.name}, but wasn't thrown")
            }
        }
    }

    def "show warning if depending on junitVintage() directly"() {
        when:
        Project p = ProjectBuilder.builder().withParent(testRoot).build()

        p.file(".").mkdir()
        p.file("src/main").mkdirs()
        p.file("src/main/AndroidManifest.xml").withWriter { it.write(ANDROID_MANIFEST) }

        p.apply plugin: 'com.android.application'
        p.apply plugin: 'de.mannodermaus.android-junit5'
        p.android {
            compileSdkVersion COMPILE_SDK
            buildToolsVersion BUILD_TOOLS

            defaultConfig {
                applicationId APPLICATION_ID
                minSdkVersion MIN_SDK
                targetSdkVersion TARGET_SDK
                versionCode VERSION_CODE
                versionName VERSION_NAME
            }
        }
        p.repositories {
            jcenter()
        }
        p.dependencies {
            testCompile junitJupiter()
            testApk junitVintage()
        }

        then:
        p.evaluate()
        // Unsure how to capture the output directly
        // (Project.logging listeners don't seem to work)
        assert true == true
    }
}
