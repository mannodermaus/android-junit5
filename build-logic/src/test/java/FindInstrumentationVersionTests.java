import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FindInstrumentationVersionTests {
    @Test
    public void findCorrectVersionForSnapshotPlugin() {
        String actual = TasksKt.findInstrumentationVersion(
                "plugin-1.0-SNAPSHOT",
                "instrumentation-2.0-SNAPSHOT",
                "instrumentation-1.0"
        );

        assertEquals("instrumentation-2.0-SNAPSHOT", actual);
    }

    @Test
    public void findCorrectVersionForStablePluginAndStableInstrumentation() {
        String actual = TasksKt.findInstrumentationVersion(
                "plugin-1.0",
                "instrumentation-2.0",
                "instrumentation-1.0"
        );

        assertEquals("instrumentation-2.0", actual);
    }

    @Test
    public void findCorrectVersionForStablePluginAndSnapshotInstrumentation() {
        String actual = TasksKt.findInstrumentationVersion(
                "plugin-1.0",
                "instrumentation-2.0-SNAPSHOT",
                "instrumentation-1.0"
        );

        assertEquals("instrumentation-1.0", actual);
    }
}
