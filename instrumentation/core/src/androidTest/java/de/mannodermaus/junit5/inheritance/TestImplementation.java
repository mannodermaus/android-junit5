package de.mannodermaus.junit5.inheritance;

public class TestImplementation extends TestExecutor {

    @Override
    public String getFilename() {
        return "test";
    }
}
