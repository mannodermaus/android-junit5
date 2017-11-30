package de.mannodermaus.junit5.test.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import de.mannodermaus.junit5.test.R

class FirstActivity : Activity() {

  lateinit var textView: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_first)

    this.textView = findViewById(R.id.textView)
    this.textView.setOnClickListener {
      setResult(RESULT_OK, Intent().apply {
        putExtra("returnValue", 1337)
      })
      finish()
    }
  }
}
