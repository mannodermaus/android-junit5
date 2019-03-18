package de.mannodermaus.junit5

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import de.mannodermaus.junit5.test.R

class TestActivity : Activity() {

  private val textView by lazy { findViewById<TextView>(R.id.textView) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_test)
  }

  fun changeText(label: String) {
    textView.text = label
  }
}
