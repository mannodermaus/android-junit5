package de.mannodermaus.app;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import android.content.Intent;

class AndroidTest {
  @Test
  void test() {
    Intent intent = new Intent();
    assertNull(intent.getAction());
  }
}
