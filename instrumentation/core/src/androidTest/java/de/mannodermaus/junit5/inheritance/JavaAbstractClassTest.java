package de.mannodermaus.junit5.inheritance;

import androidx.annotation.Nullable;

public class JavaAbstractClassTest extends JavaAbstractClass {
  @Nullable
  @Override
  public String getJavaFileName() {
    return "hello world";
  }
}
