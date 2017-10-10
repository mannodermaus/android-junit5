import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.util.Collections.unmodifiableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

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

  @BeforeAll static void runOnceBeforeTestsStart() {
    System.out.println("@BeforeAll");
  }

  @BeforeEach void runOnceBeforeEveryTest(TestInfo info) {
    System.out.println("@Before Test " + info.getDisplayName());
  }

  @AfterEach void runOnceAfterEveryTest(TestInfo info) {
    System.out.println("@After Test " + info.getDisplayName());
  }

  @AfterAll static void runOnceAfterTestsFinish() {
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

  @Test @Disabled void failingDisabledTest() {
    assertEquals(5, 2 + 2);
  }

  @Test void ordinaryTestCase() {
    assertEquals(4, 2 + 2);
  }

  @Test @Tag("Slow") void taggedTest() throws InterruptedException {
    Thread.sleep(2000);
    assertTrue(true);
  }

  @ParameterizedTest
  @ValueSource(strings = { "Android's Cool!", "JUnit 5 as well", "Acknowledgement" })
  void parameterizedTest(String value) {
    assertAll(() -> assertNotNull(value), () -> assertEquals(15, value.length()));
  }

  @Nested @DisplayName("Nested Class With Distinct Name") final class NestedTestClass {

    @Test @DisplayName("Test with Custom Display Name in nested class") void testInNestedClass() {
      assertEquals("LOL", "LO" + "L");
    }
  }

  @TestFactory Collection<DynamicTest> dynamicTestsGeneratedFromFactory() {
    String input = "Dynamic Test Input";

    List<DynamicTest> tests = new ArrayList<>();
    tests.add(dynamicTest("Length of input", () -> assertEquals(18, input.length())));
    tests.add(dynamicTest("Lowercase of input",
        () -> assertEquals("dynamic test input", input.toLowerCase())));
    tests.add(dynamicTest("Uppercase of input",
        () -> assertEquals("DYNAMIC TEST INPUT", input.toUpperCase())));

    return unmodifiableList(tests);
  }
}
