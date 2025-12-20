package de.mannodermaus.junit5.internal.compat

import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.reflect.KClass

// JUnit 5 facade of ExtensionContext.Store APIs
// that were deprecated/removed in subsequent versions of the framework.

internal fun <K : Any, V : Any> ExtensionContext.Store.computeIfAbsentCompat(
    key: K,
    defaultCreator: (K) -> V
): Any = getOrComputeIfAbsent(key, defaultCreator)

internal fun <K : Any, V : Any> ExtensionContext.Store.computeIfAbsentCompat(
    key: K,
    defaultCreator: (K) -> V,
    requiredType: KClass<V>
): V = getOrComputeIfAbsent(key, defaultCreator, requiredType.java)
