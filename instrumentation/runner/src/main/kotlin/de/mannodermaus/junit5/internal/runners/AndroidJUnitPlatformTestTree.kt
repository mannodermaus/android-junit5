package de.mannodermaus.junit5.internal.runners

import android.annotation.SuppressLint
import de.mannodermaus.junit5.internal.extensions.isDynamicTest
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.junit.platform.suite.api.SuiteDisplayName
import org.junit.platform.suite.api.UseTechnicalNames
import org.junit.runner.Description
import java.util.*
import java.util.function.Predicate

/**
 * Required, public extension to allow access to package-private TestTree class.
 * Furthermore, manipulate the test tree in a way that will fold dynamic tests
 * into the test report, without having the Android instrumentation mess up their naming.
 */
@SuppressLint("NewApi")
internal class AndroidJUnitPlatformTestTree(
    testPlan: TestPlan,
    testClass: Class<*>,
    private val isIsolatedMethodRun: Boolean
) {

    private val descriptions = mutableMapOf<TestIdentifier, Description>()
    private val modifiedTestPlan: ModifiedTestPlan = ModifiedTestPlan(testPlan)
    private val nameExtractor: (TestIdentifier) -> String = ::getTestName

    val suiteDescription = generateSuiteDescription(testPlan, testClass)

    fun getTestName(identifier: TestIdentifier): String =
        when {
            identifier.isContainer -> getTechnicalName(identifier)

            identifier.isDynamicTest -> {
                // Collect all dynamic tests' IDs from this identifier,
                // all the way up to the first non-dynamic test.
                // Collect the name of all these into a list, then finally
                // compose the final name from this list. Note that, because we
                // move upwards the test plan, the elements must be reversed
                // before the final name can be composed.
                val nameComponents = mutableListOf<String>()
                var currentNode: TestIdentifier? = identifier
                while (currentNode != null && currentNode.isDynamicTest) {
                    nameComponents.add(formatTestName(currentNode))
                    currentNode = modifiedTestPlan.getRealParent(currentNode).orElse(null)
                }

                nameComponents.reverse()

                // Android's Unified Test Platform (AGP 7.0+) is using literal test names
                // to create files when capturing Logcat output during execution.
                // Ergo, make sure that only legal characters are being used in the test names
                // (ref. https://github.com/mannodermaus/android-junit5/issues/263)
                nameComponents.joinToString(" - ")
            }

            else -> formatTestName(identifier)
        }

    private fun formatTestName(identifier: TestIdentifier): String {
        // During isolated executions, construct a technical version of the test name
        // for backwards compatibility with the JUnit 4-based instrumentation of Android,
        // stripping the brackets and parameters completely.
        // If we didn't, then running them from the IDE doesn't work for @Test methods with parameters
        // (See AndroidX's TestRequestBuilder$MethodFilter for where this is cross-referenced).
        if (isIsolatedMethodRun) {
            val reportName = identifier.legacyReportingName
            val bracketIndex = reportName.indexOf('(')
            if (bracketIndex > -1) {
                return reportName.substring(0, bracketIndex)
            }
        }

        return identifier.displayName.replace("()", "")
    }

    // Do not expose our custom TestPlan, because JUnit Platform wouldn't like that very much.
    // Only internally, use the wrapped version
    val testPlan: TestPlan
        get() = modifiedTestPlan.delegate

    fun getDescription(identifier: TestIdentifier): Description {
        return descriptions.getValue(identifier)
    }

    private fun generateSuiteDescription(testPlan: TestPlan, testClass: Class<*>): Description {
        val displayName = if (testClass.isAnnotationPresent(UseTechnicalNames::class.java)) {
            testClass.name
        } else {
            getSuiteDisplayName(testClass)
        }

        return Description.createSuiteDescription(displayName).also {
            buildDescriptionTree(it, testPlan)
        }
    }

    private fun getSuiteDisplayName(testClass: Class<*>): String =
        AnnotationUtils.findAnnotation(testClass, SuiteDisplayName::class.java)
            .map(SuiteDisplayName::value)
            .filter(String::isNotBlank)
            .orElse(testClass.name)

    private fun buildDescriptionTree(suiteDescription: Description, testPlan: TestPlan) {
        testPlan.roots.forEach { identifier ->
            buildDescription(
                identifier,
                suiteDescription,
                testPlan
            )
        }
    }

    fun addDynamicDescription(newIdentifier: TestIdentifier, parentId: String) {
        val parent = getDescription(modifiedTestPlan.getTestIdentifier(parentId))
        buildDescription(newIdentifier, parent, modifiedTestPlan)
    }

    private fun buildDescription(
        identifier: TestIdentifier,
        parent: Description,
        testPlan: TestPlan
    ) {
        val newDescription = createJUnit4Description(identifier, testPlan)
        parent.addChild(newDescription)
        descriptions[identifier] = newDescription

        testPlan.getChildren(identifier).forEach { child ->
            buildDescription(child, newDescription, testPlan)
        }
    }

    private fun createJUnit4Description(
        identifier: TestIdentifier,
        testPlan: TestPlan
    ): Description {
        val name = nameExtractor(identifier)

        return if (identifier.isTest) {
            Description.createTestDescription(
                /* className = */ testPlan.getParent(identifier)
                    .map(nameExtractor)
                    .orElse("<unrooted>"),
                /* name = */ name,
                /* uniqueId = */ identifier.uniqueId
            )
        } else {
            Description.createSuiteDescription(name, identifier.uniqueId)
        }
    }

    private fun getTechnicalName(testIdentifier: TestIdentifier): String {
        val optionalSource = testIdentifier.source

        if (optionalSource.isPresent) {
            val source = optionalSource.get()

            if (source is ClassSource) {
                return source.javaClass.name

            } else if (source is MethodSource) {
                val methodParameterTypes = source.methodParameterTypes
                return if (methodParameterTypes.isBlank()) {
                    source.methodName
                } else {
                    String.format("%s(%s)", source.methodName, methodParameterTypes)
                }
            }
        }

        // Else fall back to display name
        return testIdentifier.displayName
    }

    fun getTestsInSubtree(ancestor: TestIdentifier): Set<TestIdentifier> {
        return modifiedTestPlan.getDescendants(ancestor)
            .filter { it.isTest }
            .toSet()
    }

    /**
     * Custom drop-in TestPlan for Android purposes.
     */
    private class ModifiedTestPlan(val delegate: TestPlan) :
        TestPlan(delegate.containsTests(), delegate.configurationParameters) {

        fun getRealParent(child: TestIdentifier?): Optional<TestIdentifier> {
            // Because the overridden "getParent()" from the superclass is modified,
            // expose this additional method to access the actual parent identifier of the given child.
            // This is needed when composing the display name of a dynamic test.
            return delegate.getParent(child)
        }

        override fun getParent(child: TestIdentifier): Optional<TestIdentifier> {
            // Since parameterized tests are interpreted incorrectly by Android,
            // they access their grandparent identifier, instead of the parent like usual.
            // This causes each invocation to be grouped under the class, rather than next to it
            // using a butchered container name.
            return if (child.isDynamicTest) {
                findEligibleParentOfDynamicTest(child)
            } else {
                getRealParent(child)
            }
        }

        private fun findEligibleParentOfDynamicTest(child: TestIdentifier): Optional<TestIdentifier> {
            var node = delegate.getParent(child)
            while (node.isPresent && node.get().isDynamicTest) {
                node = node.flatMap(delegate::getParent)
            }
            return node
        }

        /* Unchanged */

        override fun addInternal(testIdentifier: TestIdentifier?) {
            delegate.addInternal(testIdentifier)
        }

        override fun getRoots(): Set<TestIdentifier> {
            return delegate.roots
        }

        override fun getChildren(parent: TestIdentifier): Set<TestIdentifier> {
            return delegate.getChildren(parent)
        }

        override fun getChildren(parentId: String): Set<TestIdentifier> {
            return delegate.getChildren(parentId)
        }

        override fun getTestIdentifier(uniqueId: String): TestIdentifier {
            return delegate.getTestIdentifier(uniqueId)
        }

        override fun countTestIdentifiers(predicate: Predicate<in TestIdentifier>): Long {
            return delegate.countTestIdentifiers(predicate)
        }

        override fun getDescendants(parent: TestIdentifier): Set<TestIdentifier> {
            return delegate.getDescendants(parent)
        }

        override fun containsTests(): Boolean {
            return delegate.containsTests()
        }
    }
}