package de.mannodermaus.gradle.anj5

import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.internal.resolve.ModuleVersionNotFoundException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class AndroidJUnitPlatformSpec extends Specification {

    private static final COMPILE_SDK = 25
    private static final BUILD_TOOLS = "25.0.2"
    private static final MIN_SDK = 25
    private static final TARGET_SDK = 25
    private static final VERSION_CODE = 1
    private static final VERSION_NAME = "1.0"
    private static final APPLICATION_ID = "org.junit.android.sample"

    /* SDK Directory, taken from the project itself and setup */
    static String sdkDir

    /* Per-test root project */
    Project testRoot

    /* Before Class */

    def setupSpec() {
        File projectRoot = new File(System.getProperty("user.dir")).parentFile
        File localProperties = new File(projectRoot, "local.properties")
        if (localProperties.exists()) {
            sdkDir = localProperties.readLines().find { it.startsWith("sdk.dir") }
        }

        if (sdkDir == null) {
            throw new AssertionError("'sdk.dir' couldn't be found. " +
                    "Either local.properties file in folder '${projectRoot.absolutePath}' is missing, " +
                    "or it doesn't include the required 'sdk.dir' statement!")
        }
    }

    /* Before Each */

    def setup() {
        // Setup an Android-like environment, pointing to the Android SDK
        // using this project's local.properties file as a baseline.
        testRoot = ProjectBuilder.builder().build()
        testRoot.file("local.properties").withWriter { it.write(sdkDir) }
    }

    /* Test Cases */

    def "requires android plugin"() {
        when:
        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()

        p.apply plugin: 'de.mannodermaus.android-junit5'

        then:
        def e = thrown(PluginApplicationException)
        e.cause.message == "The android or android-library plugin must be applied to this project"
    }

    def "basic application setup"() {
        when:
        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()

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
        p.evaluate()

        then:
        p.tasks.getByName("junitPlatformTestDebug")
        p.tasks.getByName("junitPlatformTestRelease")
    }

    def "basic library setup"() {
        when:
        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()

        // Library projects require an AndroidManifest
        def mainDir = p.file("src/main")
        mainDir.mkdirs()
        p.file("${mainDir.absolutePath}/AndroidManifest.xml").withWriter {
            it.write(
                    "<manifest package=\"$APPLICATION_ID\"></manifest>"
            )
        }

        p.apply plugin: 'com.android.library'
        p.apply plugin: 'de.mannodermaus.android-junit5'
        p.android {
            compileSdkVersion COMPILE_SDK
            buildToolsVersion BUILD_TOOLS
        }
        p.evaluate()

        then:
        p.tasks.getByName("junitPlatformTestDebug")
        p.tasks.getByName("junitPlatformTestRelease")
    }

    def "application with product flavors"() {
        when:
        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()

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

            productFlavors {
                free {
                }

                paid {
                }
            }
        }
        p.evaluate()

        then:
        p.tasks.getByName("junitPlatformTestFreeDebug")
        p.tasks.getByName("junitPlatformTestFreeRelease")
        p.tasks.getByName("junitPlatformTestPaidDebug")
        p.tasks.getByName("junitPlatformTestPaidRelease")
    }

    def "custom junit jupiter version"() {
        when:
        def nonExistentVersion = "0.0.0"

        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()

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
            testCompile junitJupiter()
        }

        then:
        try {
            p.evaluate()
            throw new AssertionError("Expected ${ModuleVersionNotFoundException.class.name}, but wasn't thrown")

        } catch (Throwable expected) {
            while (expected != null) {
                if (expected instanceof ModuleVersionNotFoundException) {
                    assert expected.message.contains("Could not find org.junit.jupiter:junit-jupiter-engine:$nonExistentVersion.")
                    break
                }
                expected = expected.cause
            }

            if (expected == null) {
                throw new AssertionError("Expected ${ModuleVersionNotFoundException.class.name}, but wasn't thrown")
            }
        }
    }

    def "junit jupiter dependency extension works"() {
        when:
        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()

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
            testCompile junitJupiter()
        }

        then:
        def testCompileDeps = p.configurations.getByName("testCompile").dependencies
        testCompileDeps.find { it.group == "org.junit.jupiter" && it.name == "junit-jupiter-engine" } != null
    }
}
