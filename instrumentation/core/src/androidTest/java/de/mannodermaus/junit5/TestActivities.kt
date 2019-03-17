package de.mannodermaus.junit5

import android.app.Activity
import android.os.Bundle
import de.mannodermaus.junit5.test.R

class TestActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_test)
  }
}
