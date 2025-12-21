package extensions

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

fun Provider<MinimalExternalModuleDependency>.getWithVersion(version: String): String {
    return this.get().toString().replace("+", version)
}
