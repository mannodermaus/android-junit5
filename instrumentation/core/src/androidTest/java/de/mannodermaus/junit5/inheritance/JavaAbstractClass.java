package de.mannodermaus.junit5.inheritance;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

abstract class JavaAbstractClass {
  @Test
  void javaTest() {
    assertNotNull(getJavaFileName());
  }

  abstract String getJavaFileName();
}
