package de.mannodermaus.gradle.plugins.android_junit5

import com.android.builder.Version
import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.internal.resolve.ModuleVersionNotFoundException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.IgnoreIf
import spock.lang.Specification

/**
 * Base class for Unit Tests of the android-junit5 plugin.
 * The structure of this project allows common unit tests
 * to be executed with different versions of the Android Gradle plugin backing it up.
 */
abstract class AndroidJUnitPlatformSpec extends Specification {

    protected static final COMPILE_SDK = 26
    protected static final BUILD_TOOLS = "26.0.1"
    protected static final MIN_SDK = 26
    protected static final TARGET_SDK = 26
    protected static final VERSION_CODE = 1
    protected static final VERSION_NAME = "1.0"
    protected static final APPLICATION_ID = "org.junit.android.sample"
    protected static final ANDROID_MANIFEST = """
        <manifest
            xmlns:android="schemas.android.com/apk/res/android"
            package="$APPLICATION_ID">
        </manifest>
    """

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

    private static boolean isAgp3x() {
        return Version.ANDROID_GRADLE_PLUGIN_VERSION.startsWith("3.")
    }

    /* Before Each */

    def setup() {
        // Setup an Android-like environment, pointing to the Android SDK
        // using this project's local.properties file as a baseline.
        testRoot = ProjectBuilder.builder().build()
        testRoot.file("local.properties").withWriter { it.write(sdkDir) }
    }

    /* Abstract */

    protected abstract String testCompileDependency()

    protected abstract String testRuntimeDependency()

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
        p.evaluate()

        then:
        p.tasks.getByName("junitPlatformTestDebug")
        p.tasks.getByName("junitPlatformTestRelease")
    }

    def "basic library setup"() {
        when:
        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()
        p.file("src/main").mkdirs()
        p.file("src/main/AndroidManifest.xml").withWriter { it.write(ANDROID_MANIFEST) }

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

    def "classpath assembled correctly"() {
        when:
        // Prepare another test project to link to
        Project testApkProject = ProjectBuilder.builder()
                .withParent(testRoot)
                .withName("library")
                .build()
        testApkProject.file(".").mkdir()
        testApkProject.file("src/main").mkdirs()
        testApkProject.file("src/main/AndroidManifest.xml").withWriter { it.write(ANDROID_MANIFEST) }

        testApkProject.apply plugin: 'com.android.library'
        testApkProject.android {
            compileSdkVersion COMPILE_SDK
            buildToolsVersion BUILD_TOOLS
        }

        Project p = ProjectBuilder.builder().withParent(testRoot).build()
        p.file(".").mkdir()
        p.file("src/main").mkdirs()
        p.file("src/main/AndroidManifest.xml").withWriter { it.write(ANDROID_MANIFEST) }

        p.apply plugin: 'com.android.application'
        p.apply plugin: 'de.mannodermaus.android-junit5'
        p.repositories {
            jcenter()
        }
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
            // "testApk" or "testRuntimeOnly"
            add(testRuntimeDependency(), testApkProject)
        }
        p.evaluate()

        then:
        // Check that all expected folders are contained in the JUnit task's classpath
        [p.tasks.getByName("junitPlatformTestDebug"),
         p.tasks.getByName("junitPlatformTestRelease")].forEach { task ->
            def classpath = task.classpath.collect { it.absolutePath }

            println "---- $task.name"
            classpath.each {
                println "  > $it"
            }

            // Source set's outputs
            assert classpath.find { it.contains("test/build/intermediates/classes/") } != null
            assert classpath.find { it.contains("test/build/intermediates/classes/test/") } != null

            // Resource Files
            assert classpath.find { it.contains("test/build/intermediates/sourceFolderJavaResources/") } != null
            assert classpath.find { it.contains("test/build/intermediates/sourceFolderJavaResources/test/") } != null

            // Mockable android.jar
            assert classpath.find { it.contains("build/generated/mockable-android-") } != null
        }
    }

    def "application with product flavors"() {
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

            flavorDimensions "plan"

            productFlavors {
                free {
                    dimension "plan"
                }

                paid {
                    dimension "plan"
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

    @IgnoreIf({ AndroidJUnitPlatformSpec.isAgp3x() })
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
            // "testCompile" or "testApi"
            invokeMethod(testCompileDependency(), junitJupiter())
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
}
