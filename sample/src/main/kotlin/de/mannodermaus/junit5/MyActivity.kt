package de.mannodermaus.junit5

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MyActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(TextView(this).apply { text = "Hello World!"})
  }
}
