package de.mannodermaus.junit5.internal.extensions

import android.util.Log
import de.mannodermaus.junit5.internal.LOG_TAG
import java.lang.reflect.Method
import java.lang.reflect.Modifier

private val jupiterTestAnnotations = listOf(
    "org.junit.jupiter.api.Test",
    "org.junit.jupiter.api.TestFactory",
    "org.junit.jupiter.api.RepeatedTest",
    "org.junit.jupiter.api.TestTemplate",
    "org.junit.jupiter.params.ParameterizedTest",
)

internal fun Class<*>.jupiterTestMethods(): Set<Method> =
    jupiterTestMethods(includeInherited = true)

private fun Class<*>.jupiterTestMethods(includeInherited: Boolean): Set<Method> = buildSet {
    try {
        // Check each method in the Class for the presence
        // of the well-known list of JUnit Jupiter annotations.
        addAll(declaredMethods.filterAnnotatedByJUnitJupiter())

        // Recursively check inner classes as well
        declaredClasses.forEach { inner ->
            addAll(inner.jupiterTestMethods(includeInherited = false))
        }

        // Attach methods from inherited superclass or (for Java) implemented interfaces, too
        if (includeInherited) {
            addAll(superclass?.jupiterTestMethods(includeInherited = true).orEmpty())
            interfaces.forEach { i -> addAll(i.jupiterTestMethods(includeInherited = true)) }
        }
    } catch (t: Throwable) {
        Log.w(LOG_TAG, "${t.javaClass.name} in 'hasJupiterTestMethods()' for $name", t)
    }
}

private fun Array<Method>.filterAnnotatedByJUnitJupiter(): List<Method> =
    filter { method ->
        // The method must not be static...
        if (method.isStatic) return@filter false

        // ...and have at least one of the recognized JUnit 5 annotations
        val names = method.declaredAnnotations.map { it.annotationClass.qualifiedName }
        jupiterTestAnnotations.any(names::contains)
    }

private val Method.isStatic get() = Modifier.isStatic(modifiers)
