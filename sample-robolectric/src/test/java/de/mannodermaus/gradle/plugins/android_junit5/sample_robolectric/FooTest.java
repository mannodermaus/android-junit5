package de.mannodermaus.gradle.plugins.android_junit5.sample_robolectric;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
final class FooTest {

    @BeforeClass
    static void runOnceBeforeTestsStart() {
        System.out.println("@BeforeClass");
    }

    @Before
    void runOnceBeforeEveryTest() {
        System.out.println("@Before Test");
    }

    @After
    void runOnceAfterEveryTest() {
        System.out.println("@After Test");
    }

    @AfterClass
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
     * n/a                                   -> @TestFactory
     */

    @Test
    @Ignore
    void failingDisabledTest() {
        assertEquals(5, 2 + 2);
    }

    @Test
    void ordinaryTestCase() {
        final int result = new Foo().bar();

        assertEquals(-1, result);
    }

    @Test
    @Category(Object.class)
    void taggedTest() throws InterruptedException {
        Thread.sleep(2000);
        assertTrue(true);
    }

}
