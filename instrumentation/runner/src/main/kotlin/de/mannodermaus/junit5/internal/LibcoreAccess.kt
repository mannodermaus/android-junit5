package de.mannodermaus.junit5.internal

import android.util.Log

internal object LibcoreAccess {

    private class Wrapper {
        private val libcoreClass = Class.forName("libcore.io.Libcore")
        private val libcoreOsObject = libcoreClass.getField("os").get(null)
        private val setEnvMethod = libcoreOsObject.javaClass.getMethod(
            "setenv",
            String::class.java,
            String::class.java,
            Boolean::class.java
        )

        fun setenv(key: String, value: String, overwrite: Boolean) {
            setEnvMethod.invoke(libcoreOsObject, key, value, overwrite)
        }
    }

    private val wrapper by lazy {
        try {
            Wrapper()
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "FATAL: Cannot initialize access to Libcore", t)
            null
        }
    }

    /**
     * Invokes the method "libcore.io.Libcore.os.setenv(String, String)" with the provided key/value pair.
     * This effectively adds a custom environment variable to the running process,
     * allowing instrumentation tests to honor JUnit 5's @EnabledIfEnvironmentVariable and @DisabledIfEnvironmentVariable annotations.
     *
     * @param key   Key of the variable
     * @param value Value of the variable
     * @throws IllegalAccessException If Libcore is not available
     */
    @Throws(IllegalAccessException::class)
    fun setenv(key: String, value: String) {
        val libcoreWrapper = this.wrapper
        if (libcoreWrapper != null) {
            libcoreWrapper.setenv(key, value, true)
        } else {
            throw IllegalAccessException("Cannot access Libcore.os.setenv()")
        }
    }
}
