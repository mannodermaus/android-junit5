package de.mannodermaus.junit5.test

import android.content.Intent
import org.junit.jupiter.api.Assertions.assertEquals

fun assertIntentHasFlag(intent: Intent, flag: Int) {
  assertEquals(
      flag,
      intent.flags and flag,
      "Expected Intent flag wasn't provided to Activity. Expected $flag, but was ${intent.flags}")
}
