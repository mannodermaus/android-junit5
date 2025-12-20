package extensions

import SupportedAgp
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("extensions.libs")

fun VersionCatalog.agp(version: SupportedAgp): String {
    return "com.android.tools.build:gradle:${version.version}"
}

val VersionCatalog.kgp: String
    get() = "org.jetbrains.kotlin:kotlin-gradle-plugin:${version("kotlin")}"

fun VersionCatalog.library(name: String): Provider<MinimalExternalModuleDependency> {
    return findLibrary(name).get()
}

fun VersionCatalog.version(name: String): String {
    return findVersion(name).get().requiredVersion
}
