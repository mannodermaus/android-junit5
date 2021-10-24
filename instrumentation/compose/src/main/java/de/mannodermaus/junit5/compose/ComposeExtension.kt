package de.mannodermaus.junit5.compose

/**
 *
 */
public interface ComposeExtension {
    /**
     *
     */
    public fun runComposeTest(block: ComposeContext.() -> Unit)
}
