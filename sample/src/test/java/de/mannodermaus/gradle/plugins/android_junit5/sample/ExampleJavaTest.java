package de.mannodermaus.gradle.plugins.android_junit5.sample;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

final class ExampleJavaTest {

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

    @BeforeAll
    static void runOnceBeforeTestsStart() {
        System.out.println("@BeforeAll");
    }

    @BeforeEach
    void runOnceBeforeEveryTest(TestInfo info) {
        System.out.println("@Before Test " + info.getDisplayName());
    }

    @AfterEach
    void runOnceAfterEveryTest(TestInfo info) {
        System.out.println("@After Test " + info.getDisplayName());
    }

    @AfterAll
    static void runOnceAfterTestsFinish() {
        System.out.println("@AfterAll");
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
     */

    @Test
    @Disabled
    void failingDisabledTest() {
        assertEquals(5, 2 + 2);
    }

    @Test
    void ordinaryTestCase() {
        assertEquals(4, 2 + 2);
    }

    @Test
    @Tag("Slow")
    void taggedTest() throws InterruptedException {
        Thread.sleep(2000);
        assertTrue(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Android's Cool!", "JUnit 5 as well", "Acknowledgement"})
    void parameterizedTest(String value) {
        assertAll(
                () -> assertNotNull(value),
                () -> assertEquals(15, value.length()));
    }

    @Nested
    @DisplayName("Nested Class With Distinct Name")
    final class NestedTestClass {

        @Test
        @DisplayName("Test with Custom Display Name in nested class")
        void testInNestedClass() {
            assertEquals("LOL", "LO" + "L");
        }
    }
}
