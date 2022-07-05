package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import de.mannodermaus.gradle.plugins.junit5.dsl.AndroidJUnitPlatformExtension
import de.mannodermaus.gradle.plugins.junit5.internal.config.EXTENSION_NAME
import org.gradle.api.Project
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal val Project.junitPlatform
    get() = extensionByName<AndroidJUnitPlatformExtension>(EXTENSION_NAME)

internal val Project.android
    get() = extensionByName<BaseExtension>("android")

@OptIn(ExperimentalContracts::class)
internal fun Project.whenAndroidPluginAdded(block: (BasePlugin) -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

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
            throw IllegalStateException("An Android plugin must be applied in order for android-junit5 to work correctly!")
        }
    }
}
