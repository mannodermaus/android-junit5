import com.ncorti.ktfmt.gradle.KtfmtExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.ktfmt).apply(false)
    alias(libs.plugins.shadow).apply(false)

    alias(libs.plugins.kotlin.binarycompvalidator)
    alias(libs.plugins.publish)
}

subprojects {
    val jvmTarget = JvmTarget.JVM_17
    val javaVersion = JavaVersion.toVersion(jvmTarget.target)

    // Configure code formatting
    apply(plugin = "com.ncorti.ktfmt.gradle")
    configure<KtfmtExtension> {
        kotlinLangStyle()
    }

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
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(libs.versions.kotlin.get())
            }
        }
    }

    // Configure Java
    plugins.withId("java") {
        configure<JavaPluginExtension> {
            toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion)) }
        }
    }

    // Configure testing
    tasks.withType<Test> {
        useJUnitPlatform()
        failFast = true
        testLogging {
            events = setOf(TestLogEvent.STARTED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
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
