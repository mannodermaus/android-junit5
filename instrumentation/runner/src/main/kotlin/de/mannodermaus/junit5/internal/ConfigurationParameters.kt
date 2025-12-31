package de.mannodermaus.junit5.internal

public object ConfigurationParameters {
    /**
     * How to behave when executing instrumentation tests on an unsupported device (i.e. too old).
     * Accepted values: "skip", "fail"
     */
    public const val BEHAVIOR_FOR_UNSUPPORTED_DEVICES: String =
        "de.mannodermaus.junit.unsupported.behavior"
}
