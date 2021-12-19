rootProject.name = "android-junit5-instrumentation"
includeBuild("../build-logic")
include(":core")
include(":runner")
include(":sample")

// Include the Compose library module only if a special flag is available.
// The 'junit5.includeCompose' flag must be present in either of the following places:
// 1) Project properties (i.e. "-Pxyz")
// 2) local.properties
val flagName = "junit5.includeCompose"

val includeCompose = run {
    if (gradle.startParameter.projectProperties[flagName]?.toBoolean() == true) {
        // 1)
        true
    } else {
        // 2)
        java.util.Properties().apply {
            File(rootProject.projectDir, "local.properties").also { load(it.inputStream() )}
        }.getProperty(flagName, null)?.toBoolean() == true
    }
}

if (includeCompose) {
    include(":compose")
}
