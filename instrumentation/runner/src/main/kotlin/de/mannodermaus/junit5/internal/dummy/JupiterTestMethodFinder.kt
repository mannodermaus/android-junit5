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
 * for devices running below the API level requirement of the JUnit Framework.
 * As they rely on Java 8 stuff, we're unable to rely on JUnit Platform's own reflection utilities.
 */
internal object JupiterTestMethodFinder {
    // Carefully access the Jupiter annotations, since it's possible that they aren't on
    // the runtime classpath (esp. "ParameterizedTest" could be absent if the consumer
    // didn't include a dependency on junit-jupiter-params)
    private val jupiterTestAnnotations = buildList {
        addSafely { Test::class.java }
        addSafely { TestFactory::class.java }
        addSafely { RepeatedTest::class.java }
        addSafely { TestTemplate::class.java }
        addSafely { ParameterizedTest::class.java }
    }

    fun find(cls: Class<*>): Set<Method> = cls.doFind(includeInherited = true)

    private fun <T> MutableList<T>.addSafely(valueCreator: () -> T) {
        try {
            add(valueCreator())
        } catch (_: NoClassDefFoundError) {
            // No-op
        }
    }

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
