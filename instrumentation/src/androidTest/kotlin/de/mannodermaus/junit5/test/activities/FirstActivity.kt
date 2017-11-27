package de.mannodermaus.junit5.test.activities

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import de.mannodermaus.junit5.test.R

class FirstActivity : Activity() {

  lateinit var textView: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_first)

    this.textView = findViewById(R.id.textView)
  }
}
