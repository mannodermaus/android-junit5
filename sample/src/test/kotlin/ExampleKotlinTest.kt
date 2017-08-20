import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ExampleKotlinTest {

    /*
     * Test Lifecycle Hooks
     *
     * JUnit 4          JUnit 5
     * ------------     -----------
     * @BeforeClass ->  @BeforeAll
     * @Before      ->  @BeforeEach
     * @After       ->  @AfterEach
     * @AfterClass  ->  @AfterAll
     */

    @Suppress("unused")
    companion object {
        @BeforeAll
        @JvmStatic
        fun runOnceBeforeTestsStart() {
            println("@BeforeAll")
        }

        @AfterAll
        @JvmStatic
        fun runOnceAfterTestsFinish() {
            println("@AfterAll")
        }
    }

    @BeforeEach
    internal fun runOnceBeforeEveryTest(info: TestInfo) {
        println("@Before Test " + info.displayName)
    }

    @AfterEach
    internal fun runOnceAfterEveryTest(info: TestInfo) {
        println("@After Test " + info.displayName)
    }

    /*
     * Test Case Definitions
     *
     * JUnit 4                                  JUnit 5
     * -------------------------------------    ---------------------------
     * @org.junit.Test                       -> @org.junit.jupiter.api.Test
     * @Ignore                               -> @Disabled
     * @Category(Class)                      -> @Tag(String)
     * @Parameters + @RunWith(Parameterized) -> @ParameterizedTest + <Source>
     * Assert.assertXXX                      -> Assertions.assertXXX
     * n/a                                   -> @DisplayName
     * n/a                                   -> @Nested
     * n/a                                   -> @TestFactory
     */

    @Test
    @Disabled
    internal fun failingDisabledTest() {
        assertEquals(5, 2 + 2)
    }

    @Test
    internal fun ordinaryTestCase() {
        assertEquals(4, 2 + 2)
    }

    @Test
    @Tag("Slow")
    @Throws(InterruptedException::class)
    internal fun taggedTest() {
        Thread.sleep(2000)
        assertTrue(true)
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("Android's Cool!", "JUnit 5 as well", "Acknowledgement"))
    internal fun parameterizedTest(value: String) {
        assertAll(
                Executable { assertNotNull(value) },
                Executable { assertEquals(15, value.length) })
    }

    @Nested
    @DisplayName("Nested Class With Distinct Name")
    internal inner class NestedTestClass {

        @Test
        @DisplayName("Test with Custom Display Name in nested class")
        internal fun testInNestedClass() {
            assertEquals("LOL", "LO" + "L")
        }
    }

    @TestFactory
    fun dynamicTestsGeneratedFromFactory(): Collection<DynamicTest> {
        val input = "Dynamic Test Input"

        return listOf(
                dynamicTest("Length of input") { assertEquals(18, input.length) },
                dynamicTest("Lowercase of input") { assertEquals("dynamic test input", input.toLowerCase()) },
                dynamicTest("Uppercase of input") { assertEquals("DYNAMIC TEST INPUT", input.toUpperCase()) })
    }
}
