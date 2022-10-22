package de.mannodermaus.junit5.inheritance;

public class JavaMixedInterfaceTest implements JavaInterface, KotlinInterface {
  @Override
  public long getJavaValue() {
    return 4815162342L;
  }

  @Override
  public int getKotlinValue() {
    return 10101010;
  }
}
