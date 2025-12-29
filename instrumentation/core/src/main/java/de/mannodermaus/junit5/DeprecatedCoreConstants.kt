@file:JvmName("DeprecatedCoreConstants")

package de.mannodermaus.junit5

@Deprecated(
    message = "Renamed to JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION",
    replaceWith = ReplaceWith("JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION")
)
public const val JUNIT5_MINIMUM_SDK_VERSION: Int = JUNIT_FRAMEWORK_MINIMUM_SDK_VERSION
