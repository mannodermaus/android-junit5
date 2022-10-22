package de.mannodermaus.junit5.inheritance;

import org.junit.jupiter.api.Test;

interface JavaInterface {
   @Test
   default void javaTest() {
      assert(getJavaValue() > 0L);
   }

   long getJavaValue();
}
