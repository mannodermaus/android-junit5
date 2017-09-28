package de.mannodermaus.gradle.plugins.android_junit5.sample_robolectric;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class FooTest {

    @Test
    public void noCodeCoverage() {
        final int result = new Foo().bar();

        assertEquals(-1, result);
    }

}
