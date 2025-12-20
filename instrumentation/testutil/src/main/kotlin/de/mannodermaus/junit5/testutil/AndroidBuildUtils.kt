package de.mannodermaus.junit5.testutil

import android.os.Build
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.testutil.reflect.getFieldReflectively
import de.mannodermaus.junit5.testutil.reflect.setStaticValue

public object AndroidBuildUtils {

    public fun withApiLevel(api: Int, block: () -> Unit) {
        withMockedStaticField<Build.VERSION>(
            fieldName = "SDK_INT",
            value = api,
            block = block,
        )
    }

    public fun withManufacturer(name: String, block: () -> Unit) {
        withMockedStaticField<Build>(
            fieldName = "MANUFACTURER",
            value = name,
            block = block,
        )
    }

    public fun withMockedInstrumentation(arguments: Bundle = Bundle(), block: () -> Unit) {
        val (oldInstrumentation, oldArguments) = try {
            InstrumentationRegistry.getInstrumentation() to InstrumentationRegistry.getArguments()
        } catch (ignored: Throwable) {
            null to null
        }

        try {
            val instrumentation = StubInstrumentation()
            InstrumentationRegistry.registerInstance(instrumentation, arguments)
            block()
        } finally {
            if (oldInstrumentation != null) {
                InstrumentationRegistry.registerInstance(oldInstrumentation, oldArguments)
            }
        }
    }

    private inline fun <reified T : Any> withMockedStaticField(
        fieldName: String,
        value: Any?,
        block: () -> Unit,
    ) {
        val field = T::class.java.getFieldReflectively(fieldName)
        val oldValue = field.get(null)

        try {
            field.setStaticValue(value)
            assertThat(field.get(null)).isEqualTo(value)
            block()
        } finally {
            field.setStaticValue(oldValue)
        }
    }
}
