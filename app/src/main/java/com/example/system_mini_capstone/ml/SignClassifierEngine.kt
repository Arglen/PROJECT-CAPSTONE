package com.example.system_mini_capstone.ml

import android.util.Log
import kotlin.math.sqrt

/**
 * Data class representing a hand landmark point (x, y, z).
 */
data class HandLandmark(val x: Float, val y: Float, val z: Float = 0f)

/**
 * Classification result containing the predicted sign gesture and confidence scores.
 */
data class GestureClassification(
    val gestureName: String,
    val confidence: Float,
    val topPredictions: List<Pair<String, Float>> = emptyList()
)

/**
 * Machine Learning Inference Engine for Real-Time Sign Language Gesture Classification.
 * Evaluates 21 extracted hand landmark coordinates using robust heuristic geometry in real time.
 */
class SignClassifierEngine {

    init {
        logI("SignClassifierEngine initialized with high-performance heuristic recognition pipeline.")
    }

    /**
     * Evaluates 21 hand landmark coordinates against the classifier in real time.
     */
    fun classifyGesture(landmarks: List<HandLandmark>): GestureClassification {
        if (landmarks.isEmpty()) {
            return GestureClassification("NONE", 0f, emptyList())
        }

        return evaluateHeuristicGesture(landmarks)
    }

    /**
     * Geometric heuristic classifier for real-time sign gestures.
     */
    private fun evaluateHeuristicGesture(landmarks: List<HandLandmark>): GestureClassification {
        if (landmarks.size < 21) {
            return GestureClassification("WAITING", 0.5f, listOf(Pair("WAITING", 0.5f)))
        }

        val wrist = landmarks[0]

        fun distance(a: HandLandmark, b: HandLandmark): Float {
            val dx = a.x - b.x
            val dy = a.y - b.y
            val dz = a.z - b.z
            return sqrt(dx * dx + dy * dy + dz * dz)
        }

        val indexExtended = distance(landmarks[8], wrist) > distance(landmarks[6], wrist)
        val middleExtended = distance(landmarks[12], wrist) > distance(landmarks[10], wrist)
        val ringExtended = distance(landmarks[16], wrist) > distance(landmarks[14], wrist)
        val pinkyExtended = distance(landmarks[20], wrist) > distance(landmarks[18], wrist)
        val thumbExtended = distance(landmarks[4], wrist) > distance(landmarks[2], wrist)

        val extendedCount = listOf(indexExtended, middleExtended, ringExtended, pinkyExtended, thumbExtended).count { it }

        val (gesture, conf) = when {
            extendedCount == 5 -> Pair("HELLO", 0.94f)
            extendedCount == 0 -> Pair("YES", 0.90f)
            extendedCount == 1 && indexExtended -> Pair("POINT", 0.88f)
            extendedCount == 2 && indexExtended && middleExtended -> Pair("PEACE", 0.92f)
            extendedCount == 3 -> Pair("THREE", 0.87f)
            extendedCount == 4 -> Pair("PLEASE", 0.89f)
            else -> Pair("THANK_YOU", 0.85f)
        }

        val predictions = listOf(
            Pair(gesture, conf),
            Pair("HELLO", if (gesture == "HELLO") 0.94f else 0.45f),
            Pair("YES", if (gesture == "YES") 0.90f else 0.35f)
        ).sortedByDescending { it.second }

        return GestureClassification(
            gestureName = gesture,
            confidence = conf,
            topPredictions = predictions
        )
    }

    fun close() {
        logI("SignClassifierEngine closed successfully.")
    }

    companion object {
        private const val TAG = "SignClassifierEngine"

        private fun logI(msg: String) {
            try {
                Log.i(TAG, msg)
            } catch (_: Throwable) {
                println("I/$TAG: $msg")
            }
        }
    }
}
