package de.mannodermaus.junit5.internal.compat

import kotlin.reflect.KClass
import org.junit.jupiter.api.extension.ExtensionContext

// JUnit 6 facade of ExtensionContext.Store APIs
// that didn't exist in previous versions of the framework.

internal fun <K : Any, V : Any> ExtensionContext.Store.computeIfAbsentCompat(
    key: K,
    defaultCreator: (K) -> V,
): Any = computeIfAbsent(key, defaultCreator)

internal fun <K : Any, V : Any> ExtensionContext.Store.computeIfAbsentCompat(
    key: K,
    defaultCreator: (K) -> V,
    requiredType: KClass<V>,
): V = computeIfAbsent(key, defaultCreator, requiredType.java)
