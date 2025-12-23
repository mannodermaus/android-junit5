package de.mannodermaus

internal object Libraries {
    enum class JUnit(val majorVersion: Int, val artifactIdSuffix: String? = null) {
        @SUPPORTED_JUNIT_VERSIONS@
    }

    object Instrumentation {
        const val version = "@INSTRUMENTATION_VERSION@"
        const val compose = "@INSTRUMENTATION_GROUP@:@INSTRUMENTATION_COMPOSE@"
        const val core = "@INSTRUMENTATION_GROUP@:@INSTRUMENTATION_CORE@"
        const val extensions = "@INSTRUMENTATION_GROUP@:@INSTRUMENTATION_EXTENSIONS@"
        const val runner = "@INSTRUMENTATION_GROUP@:@INSTRUMENTATION_RUNNER@"
    }

    const val junitPlatformLauncher = "@JUNIT_PLATFORM_LAUNCHER@"
}
