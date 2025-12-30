@file:Suppress("removal", "DEPRECATION")

package de.mannodermaus.junit5.testutil.reflect

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.security.AccessController
import java.security.PrivilegedAction
import sun.misc.Unsafe

/**
 * Adapted from Paparazzi:
 * https://github.com/cashapp/paparazzi/blob/137f5ca5f3a9949336012298a7c2838fc669c01a/paparazzi/paparazzi/src/main/java/app/cash/paparazzi/Reflections.kt
 */
public fun Class<*>.getFieldReflectively(fieldName: String): Field =
    try {
        this.getDeclaredField(fieldName).also { it.isAccessible = true }
    } catch (e: NoSuchFieldException) {
        throw RuntimeException("Field '$fieldName' was not found in class $name.")
    }

public fun Field.setStaticValue(value: Any?) {
    try {
        this.isAccessible = true
        val isFinalModifierPresent = this.modifiers and Modifier.FINAL == Modifier.FINAL
        if (isFinalModifierPresent) {
            AccessController.doPrivileged<Any?>(
                PrivilegedAction {
                    try {
                        val unsafe =
                            Unsafe::class.java.getFieldReflectively("theUnsafe").get(null) as Unsafe
                        val offset = unsafe.staticFieldOffset(this)
                        val base = unsafe.staticFieldBase(this)
                        unsafe.setFieldValue(this, base, offset, value)
                        null
                    } catch (t: Throwable) {
                        throw RuntimeException(t)
                    }
                }
            )
        } else {
            this.set(null, value)
        }
    } catch (ex: SecurityException) {
        throw RuntimeException(ex)
    } catch (ex: IllegalAccessException) {
        throw RuntimeException(ex)
    } catch (ex: IllegalArgumentException) {
        throw RuntimeException(ex)
    }
}

private fun Unsafe.setFieldValue(field: Field, base: Any, offset: Long, value: Any?) =
    when (field.type) {
        Integer.TYPE -> this.putInt(base, offset, (value as Int))
        java.lang.Short.TYPE -> this.putShort(base, offset, (value as Short))
        java.lang.Long.TYPE -> this.putLong(base, offset, (value as Long))
        java.lang.Byte.TYPE -> this.putByte(base, offset, (value as Byte))
        java.lang.Boolean.TYPE -> this.putBoolean(base, offset, (value as Boolean))
        java.lang.Float.TYPE -> this.putFloat(base, offset, (value as Float))
        java.lang.Double.TYPE -> this.putDouble(base, offset, (value as Double))
        Character.TYPE -> this.putChar(base, offset, (value as Char))
        else -> this.putObject(base, offset, value)
    }
