package de.mannodermaus.junit5.test

import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.stream.Stream

class DoesntHaveTestMethods

class HasTest {
  @Test
  fun method() {
  }
}

class HasRepeatedTest {
  @RepeatedTest(5)
  fun method(info: RepetitionInfo) {
  }
}

class HasTestFactory {
  @TestFactory
  fun method() = listOf(
      dynamicTest("a") {},
      dynamicTest("b") {}
  )
}

class HasTestTemplate {
  @TestTemplate
  @ExtendWith(ExampleInvocationContextProvider::class)
  fun method(param: String) {
  }

  class ExampleInvocationContextProvider : TestTemplateInvocationContextProvider {
    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> =
        listOf("param1", "param2")
            .map(this::context)
            .stream()

    override fun supportsTestTemplate(context: ExtensionContext) = true

    private fun context(param: String): TestTemplateInvocationContext =
        object : TestTemplateInvocationContext {
          override fun getAdditionalExtensions() = listOf(
              object : ParameterResolver {
                override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
                    parameterContext.parameter.type == String::class.java

                override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
                    param
              }
          )
        }
  }
}

class HasParameterizedTest {
  @ParameterizedTest
  @CsvSource("a", "b")
  fun method(param: String) {
  }
}

class HasInnerClassWithTest {
  @Nested
  inner class InnerClass {
    @Test
    fun method() {
    }
  }
}
