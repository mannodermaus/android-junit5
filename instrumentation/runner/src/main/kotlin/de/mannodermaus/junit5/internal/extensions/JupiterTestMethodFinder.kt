package de.mannodermaus.junit5.internal.extensions

import android.util.Log
import androidx.annotation.RequiresApi
import de.mannodermaus.junit5.internal.LOG_TAG
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.params.ParameterizedTest
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.stream.Collectors

internal abstract class JupiterTestMethodFinder {
    abstract fun find(cls: Class<*>): Set<Method>
    abstract fun hasJupiterAnnotation(method: Method): Boolean

    protected val jupiterTestAnnotations = listOf(
        Test::class.java,
        TestFactory::class.java,
        RepeatedTest::class.java,
        TestTemplate::class.java,
        ParameterizedTest::class.java,
    )

    protected fun isApplicableMethod(method: Method): Boolean {
        // The method must not be static...
        if (Modifier.isStatic(method.modifiers)) return false

        // ...and have at least one of the recognized JUnit 5 annotations
        return hasJupiterAnnotation(method)
    }

    protected fun isApplicableClass(cls: Class<*>): Boolean {
        // A class must not be private to be considered
        return !Modifier.isPrivate(cls.modifiers)
    }

    protected fun logError(cls: Class<*>, t: Throwable) {
        Log.w(
            LOG_TAG,
            "Encountered ${t.javaClass.simpleName} while finding Jupiter test methods for ${cls.name}",
            t
        )
    }
}

/**
 * Algorithm to find all methods annotated with a JUnit Jupiter annotation
 * for devices running API level 26 or above. Utilize the reflection utilities
 * of the JUnit Platform to ensure consistency with method detection during
 * the actual test execution phase
 */
@RequiresApi(26)
internal object JupiterTestMethodFinderApi26 : JupiterTestMethodFinder() {
    override fun find(cls: Class<*>): Set<Method> {
        try {
            val candidates = ReflectionSupport
                .streamNestedClasses(cls, ::isApplicableClass)
                .collect(Collectors.toSet())
                .also { it.add(cls) }

            return buildSet {
                candidates.forEach { c ->
                    addAll(
                        ReflectionSupport.findMethods(
                            c,
                            ::isApplicableMethod,
                            HierarchyTraversalMode.TOP_DOWN
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            logError(cls, t)
            return emptySet()
        }
    }

    override fun hasJupiterAnnotation(method: Method): Boolean {
        return jupiterTestAnnotations.any { annotation ->
            AnnotationSupport.isAnnotated(method, annotation)
        }
    }
}

/**
 * Algorithm to find all methods annotated with a JUnit Jupiter annotation
 * for devices running below API level 26 (i.e. those that cannot run Jupiter).
 * We're unable to rely on JUnit Platform's own reflection utilities since they rely on Java 8 stuff
 */
internal object JupiterTestMethodFinderLegacy : JupiterTestMethodFinder() {
    override fun find(cls: Class<*>): Set<Method> =
        cls.doFind(includeInherited = true)

    override fun hasJupiterAnnotation(method: Method): Boolean {
        return jupiterTestAnnotations.any { method.getAnnotation(it) != null }
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
            logError(this@doFind, t)
        }
    }
}
