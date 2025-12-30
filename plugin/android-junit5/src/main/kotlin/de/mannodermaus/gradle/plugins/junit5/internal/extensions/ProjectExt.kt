package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.gradle.BasePlugin
import de.mannodermaus.gradle.plugins.junit5.dsl.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.junit5.internal.config.EXTENSION_NAME
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.contracts.ExperimentalContracts
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

internal val Project.junitPlatform
    get() = extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)

@OptIn(ExperimentalContracts::class)
internal fun Project.whenAndroidPluginAdded(block: (BasePlugin) -> Unit) {
    val configured = AtomicBoolean(false)
    plugins.withType(BasePlugin::class.java) { plugin ->
        // Prevent duplicate configuration
        if (!configured.getAndSet(true)) {
            block(plugin)
        }
    }

    afterEvaluate {
        // If no Android plugin was applied by this point, fail
        if (!configured.get()) {
            throw IllegalStateException(
                "An Android plugin must be applied in order for android-junit5 to work correctly!"
            )
        }
    }
}

internal fun Project.hasDependency(
    configurationName: String,
    matching: (Dependency) -> Boolean,
): Boolean {
    val configuration = project.configurations.getByName(configurationName)

    return configuration.dependencies.any(matching)
}

internal fun Project.usesJUnitJupiterIn(configurationName: String): Boolean {
    return project.hasDependency(configurationName) {
        // todo also check for org.junit:junit-bom
        it.group == "org.junit.jupiter" && it.name == "junit-jupiter-api"
    }
}

internal fun Project.usesComposeIn(configurationName: String): Boolean {
    return project.hasDependency(configurationName) {
        it.group?.startsWith("androidx.compose") ?: false
    }
}
