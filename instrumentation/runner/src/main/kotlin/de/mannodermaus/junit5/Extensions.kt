package de.mannodermaus.junit5

import android.util.Log
import java.lang.reflect.Method

/**
 * Created by Marcel Schnelle on 2019-03-16.
 */

const val LOG_TAG = "AndroidJUnit5"
private val jupiterTestAnnotations = listOf(
        "org.junit.jupiter.api.Test",
        "org.junit.jupiter.api.TestFactory",
        "org.junit.jupiter.params.ParameterizedTest")

fun Class<*>.jupiterTestMethods(): List<Method> {
    val allJupiterMethods = mutableListOf<Method>()
    try {

        // Check each method in the Class for the presence
        // of the well-known list of JUnit Jupiter annotations
        allJupiterMethods += declaredMethods.filter { method ->
            val annotationClassNames = method.declaredAnnotations.map { it.annotationClass.qualifiedName }
            jupiterTestAnnotations.firstOrNull { annotation ->
                annotationClassNames.contains(annotation)
            } != null
        }

        // Recursively check inner classes as well
        declaredClasses.forEach { inner ->
            allJupiterMethods += inner.jupiterTestMethods()
        }

    } catch (t: Throwable) {
        Log.w(LOG_TAG, "${t.javaClass.name} in 'hasJupiterTestMethods()' for $name", t)
    }

    return allJupiterMethods
}