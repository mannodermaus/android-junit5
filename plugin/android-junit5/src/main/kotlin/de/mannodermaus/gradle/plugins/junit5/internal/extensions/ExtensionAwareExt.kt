package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.plugins.ExtensionAware

internal inline fun <reified T> ExtensionAware.extend(
    name: String,
    args: Array<Any> = emptyArray(),
    noinline init: ((T) -> Unit)? = null
): T = extensions.create(name, T::class.java, *args).also {
    init?.invoke(it)
}

@Suppress("UNCHECKED_CAST")
internal fun <T> ExtensionAware.extensionByName(name: String): T {
    return this.extensions.getByName(name) as T
}
