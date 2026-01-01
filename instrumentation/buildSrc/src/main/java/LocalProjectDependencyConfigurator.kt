import org.gradle.api.Project

// Use local project dependencies on android-test instrumentation libraries
// for the modules inside this repository, instead of relying on their Maven coordinates.
// This is not needed for normal consumers, but we want it to test our own code.
private val instrumentationLibraryRegex =
    Regex("de\\.mannodermaus\\.junit5:android-test-([a-z0-9]+)(-.+)?:")

fun Project.replaceAndroidTestLibsWithLocalProjectDependencies() {
    val self = this

    configurations.all {
        if ("DebugAndroidTestRuntimeClasspath" in name) {
            resolutionStrategy.dependencySubstitution.all {
                instrumentationLibraryRegex.find(requested.toString())?.let { result ->
                    val replacement = project(":${result.groupValues[1]}")
                    println(
                        "In $self, replace androidTest dependency '$requested' with $replacement"
                    )
                    useTarget(replacement)
                }
            }
        }
    }
}
