import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.withGroovyBuilder

// Based on code by Chao Zhang, but updated to avoid direct dependency on Kotlin Gradle plugin:
// https://youtrack.jetbrains.com/issue/KT-37652

private const val EXPLICIT_API = "-Xexplicit-api=strict"

class ExplicitApiModePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks
            .matching { it.isKotlinCompileTask() }
            .configureEach {
                if (!project.hasProperty("kotlin.optOutExplicitApi")) {
                    enableExplicitApiMode()
                }
            }
    }

    private fun Task.isKotlinCompileTask(): Boolean {
        return "org.jetbrains.kotlin.gradle.tasks.KotlinCompile" in javaClass.name &&
                !name.contains("test", ignoreCase = true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Task.enableExplicitApiMode() {
        withGroovyBuilder {
            "kotlinOptions" {
                val freeCompilerArgs = getProperty("freeCompilerArgs") as Collection<String>
                if (EXPLICIT_API !in freeCompilerArgs) {
                    invokeMethod("setFreeCompilerArgs", freeCompilerArgs + listOf(EXPLICIT_API))
                }
            }
        }
    }
}
