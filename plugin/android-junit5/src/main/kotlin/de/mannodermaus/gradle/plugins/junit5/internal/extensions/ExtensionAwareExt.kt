package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.plugins.ExtensionAware

@Suppress("UNCHECKED_CAST")
internal fun <T> ExtensionAware.extensionByName(name: String): T {
    return this.extensions.getByName(name) as T
}
