package de.mannodermaus

internal object Libraries {
    const val instrumentationVersion = "@INSTRUMENTATION_VERSION@"
    const val instrumentationCompose = "@INSTRUMENTATION_GROUP@:@INSTRUMENTATION_COMPOSE@"
    const val instrumentationCore = "@INSTRUMENTATION_GROUP@:@INSTRUMENTATION_CORE@"
    const val instrumentationExtensions = "@INSTRUMENTATION_GROUP@:@INSTRUMENTATION_EXTENSIONS@"
    const val instrumentationRunner = "@INSTRUMENTATION_GROUP@:@INSTRUMENTATION_RUNNER@"

    const val junitPlatformLauncher = "@JUNIT_PLATFORM_LAUNCHER@"
}
