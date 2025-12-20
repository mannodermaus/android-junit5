import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.BaseExtension
import extensions.capitalized
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.android.app).version(SupportedAgp.newestStable.version).apply(false)
    alias(libs.plugins.android.junit).version(Artifacts.Plugin.latestStableVersion).apply(false)
    alias(libs.plugins.android.library).version(SupportedAgp.newestStable.version).apply(false)

    alias(libs.plugins.compose).apply(false)
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)

    alias(libs.plugins.kotlin.binarycompvalidator)
    alias(libs.plugins.publish)
}

apiValidation {
    ignoredPackages.add("de.mannodermaus.junit5.internal")
    ignoredPackages.add("de.mannodermaus.junit5.compose.internal")
    ignoredProjects.add("sample")
    ignoredProjects.add("testutil")
    ignoredProjects.add("testutil-reflect")
}

subprojects {
    apply(plugin = "explicit-api-mode")

    val jvmTarget = JvmTarget.JVM_21
    val javaVersion = JavaVersion.toVersion(jvmTarget.target)

    // Configure Kotlin
    plugins.withType<KotlinBasePlugin> {
        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            compilerOptions {
                this.progressiveMode.set(true)
                if (this is KotlinJvmCompilerOptions) {
                    this.jvmTarget.set(jvmTarget)
                }
            }
        }
    }

    // Configure Java
    plugins.withId("java") {
        configure<JavaPluginExtension> {
            toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion)) }
        }
    }

    // Configure Android
    plugins.withId("com.android.base") {
        configure<BaseExtension> {
            compileSdkVersion(Android.compileSdkVersion)

            defaultConfig {
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility(javaVersion)
                targetCompatibility(javaVersion)
            }

            with(buildFeatures) {
                buildConfig = false
                resValues = false
            }

            testOptions {
                unitTests.isReturnDefaultValues = true
            }

            // Create product flavors for each supported generation of JUnit,
            // then declare the corresponding BOM for each of them
            // to provide dependencies to each target
            val supportedTargets = SupportedJUnit.values()

            flavorDimensions("target")
            productFlavors {
                supportedTargets.forEachIndexed { index, junit ->
                    register(junit.label) {
                        dimension = "target"
                        isDefault = index == supportedTargets.lastIndex
                    }
                }
            }

            dependencies {
                supportedTargets.forEach { junit ->
                    val configNames = listOf(
                        "${junit.label}Api",
                        "${junit.label}Implementation",
                        "${junit.label}CompileOnly",
                        "${junit.label}RuntimeOnly",
                        "test${junit.label.capitalized()}Implementation",
                        "test${junit.label.capitalized()}CompileOnly",
                        "test${junit.label.capitalized()}RuntimeOnly",
                        "androidTest${junit.label.capitalized()}Implementation",
                        "androidTest${junit.label.capitalized()}CompileOnly",
                        "androidTest${junit.label.capitalized()}RuntimeOnly",
                    )

                    configNames.forEach { configName ->
                        add(
                            configurationName = configName,
                            dependencyNotation = when (junit) {
                                SupportedJUnit.JUnit5 -> platform(libs.junit.framework.bom5)
                                SupportedJUnit.JUnit6 -> platform(libs.junit.framework.bom6)
                            }
                        )
                    }
                }
            }

            if (this is LibraryExtension) {
                lint {
                    // JUnit 4 refers to java.lang.management APIs, which are absent on Android.
                    warning.add("InvalidPackage")
                    targetSdk = Android.targetSdkVersion
                }

                packaging {
                    resources.excludes.add("META-INF/AL2.0")
                    resources.excludes.add("META-INF/LGPL2.1")
                    resources.excludes.add("META-INF/LICENSE.md")
                    resources.excludes.add("META-INF/LICENSE-notice.md")
                }

                testOptions {
                    targetSdk = Android.targetSdkVersion
                }
            }
        }
    }

    // Configure testing
    tasks.withType<Test> {
        failFast = true
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
