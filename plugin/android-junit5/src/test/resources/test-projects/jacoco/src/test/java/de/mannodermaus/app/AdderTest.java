package de.mannodermaus.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavaTest {
    @Test
    void test() {
        Adder adder = new Adder();
        assertEquals(4, adder.add(2, 2), "This should succeed!");
    }
}
