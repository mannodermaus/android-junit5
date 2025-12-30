package de.mannodermaus.sample

import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

class TestTemplateExampleTests {
    @TestTemplate
    @ExtendWith(NameAndLengthTemplateContextProvider::class)
    fun testTemplate(testCase: TemplateTestCase) {
        assertEquals(testCase.expectedLength, testCase.name.length)
    }
}

data class TemplateTestCase(val name: String, val expectedLength: Int)

class NameAndLengthTemplateContextProvider : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        return true
    }

    override fun provideTestTemplateInvocationContexts(
        context: ExtensionContext
    ): Stream<TestTemplateInvocationContext> {
        return Stream.of(createCase("Alice", 5), createCase("Bob", 3))
    }

    private fun createCase(name: String, expected: Int): TestTemplateInvocationContext {
        val testCase = TemplateTestCase(name, expected)

        return object : TestTemplateInvocationContext {
            override fun getDisplayName(invocationIndex: Int): String {
                return "${testCase.name} has ${testCase.expectedLength} letters"
            }

            override fun getAdditionalExtensions(): List<Extension> {
                return listOf(
                    object : TypeBasedParameterResolver<TemplateTestCase>() {
                        override fun resolveParameter(
                            parameterContext: ParameterContext,
                            extensionContext: ExtensionContext,
                        ): TemplateTestCase {
                            return testCase
                        }
                    }
                )
            }
        }
    }
}
