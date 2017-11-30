package de.mannodermaus.junit5.test.activities

import android.app.Activity
import android.os.Bundle
import de.mannodermaus.junit5.test.R

class OtherActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_other)
  }
}
