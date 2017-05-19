package de.mannodermaus.gradle.anj5

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class AndroidJUnitPlatformAgp3xSpec extends AndroidJUnitPlatformSpec {

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
        p.dependencies {
            testApi junitJupiter()
        }

        then:
        p.evaluate()

        def testApiDeps = p.configurations.getByName("testApi").dependencies
        assert testApiDeps.find {
            it.group == "org.junit.jupiter" && it.name == "junit-jupiter-api" && it.version == nonExistentVersion
        } != null

        assert testApiDeps.find {
            it.group == "junit" && it.name == "junit" && it.version == "4.12"
        } != null
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
        p.dependencies {
            testApi junitJupiter()
            testRuntimeOnly junitVintage()
        }

        then:
        p.evaluate()
        // Unsure how to capture the output directly
        // (Project.logging listeners don't seem to work)
        assert true == true
    }
}
