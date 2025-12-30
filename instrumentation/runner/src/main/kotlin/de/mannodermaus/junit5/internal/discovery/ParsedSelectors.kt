package de.mannodermaus.junit5.internal.discovery

import android.os.Bundle
import de.mannodermaus.junit5.internal.runners.AndroidJUnitFramework
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.discovery.DiscoverySelectors

/**
 * Holder object for the selectors of a test plan.
 * It converts the arguments handed to the Runner by the
 * Android instrumentation into JUnit Platform [DiscoverySelector] objects
 * for the [AndroidJUnitFramework] runner.
 */
internal object ParsedSelectors {
    fun fromBundle(testClass: Class<*>, arguments: Bundle): List<DiscoverySelector> {
        // Check if specific class arguments were given to the Runner
        arguments.getString("class", null)?.let { classArg ->
            val testClassName = testClass.name
            val methods = testClass.declaredMethods

            val selectors = mutableListOf<DiscoverySelector>()

            // Separate the provided argument into methods, if any are given
            // (Format: class=com.package1.FirstTest#method1,com.package1.SecondTest#method2).
            // For each component in this string that applies to the test class at hand,
            // consider it a method filter if the name is appended to the component, using a pound sign (#).
            // Finally, if at least one of these method filters can be found, construct JUnit selectors from it
            classArg.split(",")
                .forEach { component ->
                    if (!component.startsWith(testClassName)) {
                        // Not the desired class
                        return@forEach
                    }

                    // Try extracting an appended method name
                    var methodName = component.replace(testClassName, "")
                    if (!methodName.startsWith("#")) {
                        return@forEach
                    }
                    methodName = methodName.substring(1)

                    // Find all methods with the given name
                    val eligibleMethods = methods
                        .filter { it.name == methodName }
                        .map { method -> DiscoverySelectors.selectMethod(testClass, method) }

                    selectors += eligibleMethods
                }

            if (selectors.isNotEmpty()) {
                // Restrictions to specific methods apply
                return selectors
            }
        }

        // If nothing else was specified to the runner, assume that all classes should be run
        return listOf(DiscoverySelectors.selectClass(testClass))
    }
}
