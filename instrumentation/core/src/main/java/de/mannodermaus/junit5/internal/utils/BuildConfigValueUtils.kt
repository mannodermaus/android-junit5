package de.mannodermaus.junit5.internal.utils

import android.annotation.SuppressLint
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import de.mannodermaus.junit5.internal.LOG_TAG
import java.lang.reflect.Field

@SuppressLint("NewApi")
internal object BuildConfigValueUtils {

    private class Wrapper {
        private val fieldCache = mutableMapOf<String, Field>()
        private val buildConfigClass = run {
            val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
            val buildConfigClassName = "$packageName.BuildConfig"
            Class.forName(buildConfigClassName)
        }

        fun getValue(key: String): String? {
            return try {
                fieldCache
                    .getOrPut(key) {
                        buildConfigClass.getField(key).also { it.isAccessible = true }
                    }
                    .get(null)
                    ?.toString()
            } catch (ignored: Throwable) {
                throw IllegalAccessException("Cannot access BuildConfig field '$key'")
            }
        }
    }

    private val wrapper by lazy {
        try {
            Wrapper()
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "Cannot initialize access to BuildConfig", t)
            null
        }
    }

    /**
     * Reflectively look up a BuildConfig field's value. This caches previous lookups to maximize
     * performance.
     *
     * @param key Key of the entry to obtain
     * @return The value of this entry, if any
     */
    @Throws(IllegalAccessException::class)
    fun getAsString(key: String): String? {
        val buildConfigWrapper = this.wrapper
        if (buildConfigWrapper != null) {
            return buildConfigWrapper.getValue(key)
        } else {
            throw IllegalAccessException("Cannot access BuildConfig field'$key'")
        }
    }
}
