package com.example.system_mini_capstone

import android.app.Application
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Custom Application class ensuring safe initialization of Firebase services
 * and a built-in Crash Diagnostics screen to display startup errors directly on screen.
 */
class SystemMiniCapstoneApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.i(TAG, "Firebase initialized successfully in Application onCreate")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization warning: ${e.message}", e)
        }

        // Global uncaught exception handler to display crash details directly on the phone screen
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTraceString = sw.toString()

                Log.e(TAG, "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)

                val intent = Intent(this, CrashDebugActivity::class.java).apply {
                    putExtra("EXTRA_ERROR_TRACE", stackTraceString)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch CrashDebugActivity", e)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {
        private const val TAG = "SystemMiniCapstoneApp"
    }
}
