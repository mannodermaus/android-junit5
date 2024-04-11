package de.mannodermaus.junit5

import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
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

class HasTaggedTest {
  @Tag("slow")
  @Test
  fun method() {

  }
}

abstract class AbstractTestClass {
  @Test
  fun abstractTest() {
  }
}

interface AbstractTestInterface {
  @Test
  fun interfaceTest() {
  }
}

class HasInheritedTestsFromClass : AbstractTestClass() {
  @Test
  fun method() {
  }
}

class HasInheritedTestsFromInterface : AbstractTestInterface

// These tests should not be acknowledged,
// as classes with legacy tests & top-level tests
// are unsupported by JUnit 5

class HasJUnit4Tests {
  @org.junit.Test
  fun method() {}
}

@RepeatedTest(2)
fun topLevelRepeatedTest(unused: RepetitionInfo) {}

@ValueSource(strings = ["a", "b"])
@ParameterizedTest
fun topLevelParameterizedTest(unused: String) {}

@TestTemplate
fun topLevelTestTemplate() {}

@TestFactory
fun topLevelTestFactory(): Stream<DynamicNode> = Stream.empty()
