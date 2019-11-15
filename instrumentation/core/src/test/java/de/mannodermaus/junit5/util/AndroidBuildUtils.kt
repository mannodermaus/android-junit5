package de.mannodermaus.junit5.util

import android.os.Build
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

object AndroidBuildUtils {

  fun withApiLevel(api: Int, block: () -> Unit) {
    try {
      assumeApiLevel(api)
      block()
    } finally {
      resetApiLevel()
    }
  }

  fun withManufacturer(name: String, block: () -> Unit) {
    try {
      assumeManufacturer(name)
      block()
    } finally {
      resetManufacturer()
    }
  }

  private fun setWithReflection(clazz: KClass<*>, fieldName: String, value: Any?) {
    // Adjust the value of the target field statically using reflection
    val field = clazz.java.getDeclaredField(fieldName)
    field.isAccessible = true

    // Temporarily remove the field's "final" modifier
    val modifiersField = Field::class.java.getDeclaredField("modifiers")
    modifiersField.isAccessible = true
    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

    // Apply the value to the field, re-finalize it, then lock it again
    field.set(null, value)
    modifiersField.setInt(field, field.modifiers or Modifier.FINAL)
    field.isAccessible = false
  }

  private fun assumeApiLevel(apiLevel: Int) {
    setWithReflection(Build.VERSION::class, "SDK_INT", apiLevel)
    assertThat(Build.VERSION.SDK_INT).isEqualTo(apiLevel)
  }

  private fun resetApiLevel() {
    assumeApiLevel(0)
  }

  private fun assumeManufacturer(name: String?) {
    setWithReflection(Build::class, "MANUFACTURER", name)
    assertThat(Build.MANUFACTURER).isEqualTo(name)
  }

  private fun resetManufacturer() {
    assumeManufacturer(null)
  }
}
