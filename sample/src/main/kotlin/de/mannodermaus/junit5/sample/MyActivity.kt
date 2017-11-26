package de.mannodermaus.junit5.sample

import android.app.Activity
import android.os.Bundle

class MyActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }
}
