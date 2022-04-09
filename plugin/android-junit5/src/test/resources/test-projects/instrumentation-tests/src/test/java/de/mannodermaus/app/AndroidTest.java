package de.mannodermaus.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import java.io.InputStream;

class AndroidTest {
  @Test
  void test() {
    InputStream is = getClass().getResourceAsStream("/com/android/tools/test_config.properties");
    assertNotNull(is);
  }
}
