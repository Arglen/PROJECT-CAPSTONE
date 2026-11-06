package com.example.system_mini_capstone.data.cloud

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.perf.FirebasePerformance
import kotlinx.coroutines.tasks.await

data class CloudLessonPayload(
    val id: String = "",
    val signName: String = "",
    val description: String = "",
    val category: String = "",
    val targetLandmarksDescription: String = "",
    val mediaUrl: String = "",
    val isPublished: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AdminActivityLog(
    val adminEmail: String,
    val actionType: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Cloud Database & Content Synchronization Manager.
 * Connects to Firebase Firestore to store user accounts, admin activity logs, system configurations,
 * and executes a dynamic cloud sync pipeline for LCMS lessons and media assets without app re-installation.
 * Also integrates Firebase Crashlytics error logging and Performance Monitoring.
 */
class CloudSyncManager(private val context: Context) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization warning: ${e.message}")
            null
        }
    }

    private val crashlytics: FirebaseCrashlytics? by lazy {
        try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Synchronizes and fetches newly published lessons and media assets from Firestore LCMS.
     */
    suspend fun fetchPublishedLessonsFromCloud(): List<CloudLessonPayload> {
        val trace = FirebasePerformance.getInstance().newTrace("fetch_cloud_lessons_trace")
        trace.start()
        try {
            val db = firestore ?: return getFallbackLessons()
            val snapshot = db.collection("lcms_lessons")
                .whereEqualTo("isPublished", true)
                .get()
                .await()

            val lessons = mutableListOf<CloudLessonPayload>()
            for (doc in snapshot.documents) {
                val lesson = doc.toObject(CloudLessonPayload::class.java)
                if (lesson != null) {
                    lessons.add(lesson)
                }
            }
            Log.i(TAG, "Successfully synced ${lessons.size} lessons from cloud Firestore")
            trace.stop()
            return if (lessons.isNotEmpty()) lessons else getFallbackLessons()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing lessons from cloud Firestore: ${e.message}", e)
            crashlytics?.recordException(e)
            trace.stop()
            return getFallbackLessons()
        }
    }

    /**
     * Logs administrator activity to Firestore activity audit collection.
     */
    fun logAdminActivity(adminEmail: String, actionType: String, details: String) {
        try {
            val db = firestore ?: return
            val logEntry = AdminActivityLog(adminEmail, actionType, details)
            db.collection("admin_activity_logs")
                .add(logEntry)
                .addOnSuccessListener {
                    Log.i(TAG, "Admin activity logged to Firestore successfully")
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Failed to log admin activity to Firestore: ${exception.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording admin activity log: ${e.message}")
            crashlytics?.recordException(e)
        }
    }

    /**
     * Synchronizes system configuration and vision sensitivity thresholds from cloud.
     */
    suspend fun fetchSystemConfigFromCloud(configKey: String): Map<String, Any>? {
        try {
            val db = firestore ?: return null
            val doc = db.collection("system_configs").document(configKey).get().await()
            return doc.data
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching system config from Firestore: ${e.message}")
            crashlytics?.recordException(e)
            return null
        }
    }

    /**
     * Reports non-fatal errors or caught exceptions to Firebase Crashlytics.
     */
    fun logNonFatalError(tag: String, message: String, throwable: Throwable?) {
        try {
            Log.e(tag, message, throwable)
            crashlytics?.log("[$tag] $message")
            if (throwable != null) {
                crashlytics?.recordException(throwable)
            } else {
                crashlytics?.recordException(RuntimeException("[$tag] $message"))
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    private fun getFallbackLessons(): List<CloudLessonPayload> {
        return listOf(
            CloudLessonPayload("1", "Hello", "Standard greeting sign gesture.", "Greetings", "Landmarks 4, 8, 12, 16, 20 extended vertically.", "", true),
            CloudLessonPayload("2", "Thank You", "Expressing gratitude gesture.", "Common", "Fingertips touching chin then moving forward.", "", true),
            CloudLessonPayload("3", "Yes", "Affirmative nodding fist gesture.", "Responses", "Closed fist nodding up and down.", "", true),
            CloudLessonPayload("4", "No", "Negative index and middle finger snap.", "Responses", "Index and middle fingers tapping thumb.", "", true)
        )
    }

    companion object {
        private const val TAG = "CloudSyncManager"
    }
}
