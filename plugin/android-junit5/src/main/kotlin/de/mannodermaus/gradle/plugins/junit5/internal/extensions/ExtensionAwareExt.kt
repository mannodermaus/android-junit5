package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.plugins.ExtensionAware

internal inline fun <reified T> ExtensionAware.extend(
        name: String,
        args: Array<Any> = emptyArray(),
        noinline init: ((T) -> Unit)? = null): T {
    val created: T = this.extensions.create(name, T::class.java, *args)
    init?.let { it(created) }
    return created
}

internal fun ExtensionAware.extensionExists(name: String): Boolean {
    return this.extensions.findByName(name) != null
}

@Suppress("UNCHECKED_CAST")
internal fun <T> ExtensionAware.extensionByName(name: String): T {
    return this.extensions.getByName(name) as T
}
