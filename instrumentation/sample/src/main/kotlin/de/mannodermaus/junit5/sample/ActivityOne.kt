package de.mannodermaus.junit5.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

public class ActivityOne : Activity() {

    private val textView by lazy { findViewById<TextView>(R.id.textView) }
    private val button by lazy { findViewById<Button>(R.id.button) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one)

        button.setOnClickListener {
            val currentClickCount = getClickCount()
            val newClickCount = currentClickCount + 1
            textView.text = newClickCount.toString()
        }
    }

    public fun getClickCount(): Int {
        return textView.text.toString().toInt()
    }

    public fun setButtonLabel(newText: String) {
        button.text = newText
    }
}
