package com.example.system_mini_capstone

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class CrashDebugActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val errorTrace = intent.getStringExtra("EXTRA_ERROR_TRACE") ?: "Unknown error stack trace."

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            setBackgroundColor(Color.parseColor("#3B0000"))
        }

        val titleView = TextView(this).apply {
            text = "⚠️ App Startup Crash Caught"
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(titleView)

        val descView = TextView(this).apply {
            text = "Please screenshot or read this error message:"
            setTextColor(Color.LTGRAY)
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(descView)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(Color.BLACK)
            setPadding(20, 20, 20, 20)
        }

        val traceView = TextView(this).apply {
            text = errorTrace
            setTextColor(Color.GREEN)
            textSize = 12f
        }
        scrollView.addView(traceView)
        layout.addView(scrollView)

        val closeButton = Button(this).apply {
            text = "Close App"
            setOnClickListener { finishAffinity() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 30
            }
        }
        layout.addView(closeButton)

        setContentView(layout)
    }
}
