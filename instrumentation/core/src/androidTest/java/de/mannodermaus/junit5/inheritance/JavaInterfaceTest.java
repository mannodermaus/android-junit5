package de.mannodermaus.junit5.inheritance;

public class JavaInterfaceTest implements JavaInterface {
  @Override
  public long getJavaValue() {
    return 4815162342L;
  }
}
