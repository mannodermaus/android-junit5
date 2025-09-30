package de.mannodermaus.junit5.internal.dummy

import android.util.Log
import de.mannodermaus.junit5.internal.LOG_TAG
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.params.ParameterizedTest
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Algorithm to find all methods annotated with a JUnit Jupiter annotation
 * for devices running below API level 35 (i.e. those that cannot run Jupiter).
 * We're unable to rely on JUnit Platform's own reflection utilities,
 * since they rely on new Java APIs that are unavailable on this device
 */
internal object JupiterTestMethodFinder {
    private val jupiterTestAnnotations = listOf(
        Test::class.java,
        TestFactory::class.java,
        RepeatedTest::class.java,
        TestTemplate::class.java,
        ParameterizedTest::class.java,
    )

    fun find(cls: Class<*>): Set<Method> = cls.doFind(includeInherited = true)

    private fun Class<*>.doFind(includeInherited: Boolean): Set<Method> = buildSet {
        try {
            // Check each method in the Class for the presence
            // of the well-known list of JUnit Jupiter annotations.
            addAll(declaredMethods.filter(::isApplicableMethod))

            // Recursively check non-private inner classes as well
            declaredClasses.filter(::isApplicableClass).forEach { inner ->
                addAll(inner.doFind(includeInherited = false))
            }

            // Attach methods from inherited superclass or (for Java) implemented interfaces, too
            if (includeInherited) {
                addAll(superclass?.doFind(includeInherited = true).orEmpty())
                interfaces.forEach { i -> addAll(i.doFind(includeInherited = true)) }
            }
        } catch (t: Throwable) {
            Log.w(
                LOG_TAG,
                "Encountered ${t.javaClass.simpleName} while finding Jupiter test methods for ${this@doFind.name}",
                t
            )
        }
    }

    private fun isApplicableMethod(method: Method): Boolean {
        // The method must not be static...
        if (Modifier.isStatic(method.modifiers)) return false

        // ...and have at least one of the recognized JUnit 5 annotations
        return hasJupiterAnnotation(method)
    }

    private fun hasJupiterAnnotation(method: Method): Boolean {
        return jupiterTestAnnotations.any { method.getAnnotation(it) != null }
    }

    private fun isApplicableClass(cls: Class<*>): Boolean {
        // A class must not be private to be considered
        return !Modifier.isPrivate(cls.modifiers)
    }
}
