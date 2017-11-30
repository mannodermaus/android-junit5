package de.mannodermaus.junit5

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent

private const val FIELD_RESULT_CODE = "mResultCode"
private const val FIELD_RESULT_DATA = "mResultData"

/**
 * Helper property to access the result of a given [Activity].
 * This code is mirroring ActivityTestRule#getActivityResult(),
 * and will need to be updated in case it blows up on a new
 * API version because of internal changes to the structure of an Activity.
 */
internal val Activity.result: ActivityResult
  get() {
    if (!this.isFinishing) {
      throw IllegalStateException("Activity is not finishing!")
    }

    try {
      val resultCodeField = Activity::class.java.getDeclaredField(FIELD_RESULT_CODE)
      resultCodeField.isAccessible = true

      val resultDataField = Activity::class.java.getDeclaredField(FIELD_RESULT_DATA)
      resultDataField.isAccessible = true

      return ActivityResult(
          resultCodeField.get(this) as Int,
          resultDataField.get(this) as Intent?)

    } catch (e: NoSuchFieldException) {
      throw RuntimeException(
          "Looks like the Android Activity class has changed its " +
              "private fields for mResultCode or mResultData. " +
              "Time to update the reflection code.", e)

    } catch (e: IllegalAccessException) {
      throw RuntimeException("Field mResultCode or mResultData is not accessible", e)
    }
  }
