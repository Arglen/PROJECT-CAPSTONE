package com.example.system_mini_capstone

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class CrashDebugActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDebug = BuildConfig.DEBUG
        val errorTrace = intent.getStringExtra("EXTRA_ERROR_TRACE") ?: "Unknown error stack trace."

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            setBackgroundColor(Color.parseColor(if (isDebug) "#3B0000" else "#121212"))
        }

        val titleView = TextView(this).apply {
            text = if (isDebug) "⚠️ App Startup Crash Caught" else "Something Went Wrong"
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(titleView)

        val descView = TextView(this).apply {
            text = if (isDebug) "Please screenshot or read this error message:" else "A temporary technical issue occurred. Please restart the application."
            setTextColor(Color.LTGRAY)
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(descView)

        if (isDebug) {
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
        }

        val restartButton = Button(this).apply {
            text = if (isDebug) "Close App" else "Restart App"
            setOnClickListener {
                if (isDebug) {
                    finishAffinity()
                } else {
                    val intent = Intent(this@CrashDebugActivity, SplashActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 30
            }
        }
        layout.addView(restartButton)

        setContentView(layout)
    }
}
