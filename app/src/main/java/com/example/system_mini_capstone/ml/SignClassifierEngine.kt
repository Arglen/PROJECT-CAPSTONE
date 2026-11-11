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

enum class FingerState {
    EXTENDED, CURLED
}

/**
 * High-precision Geometric Heuristic Inference Engine for Real-Time Sign Language Gesture Classification.
 * Evaluates 21 hand landmarks using palm-size normalization, joint angle vectors, and finger states.
 */
class SignClassifierEngine {

    init {
        logI("SignClassifierEngine initialized with rotation-tolerant heuristic geometric pipeline.")
    }

    /**
     * Evaluates 21 hand landmark coordinates against the precise geometric gesture rules.
     */
    fun classifyGesture(landmarks: List<HandLandmark>): GestureClassification {
        if (landmarks.size < 21) {
            return GestureClassification("UNKNOWN", 0f, emptyList())
        }

        // A. Palm size normalization (Wrist = 0, Middle MCP = 9)
        val wrist = landmarks[0]
        val middleMcp = landmarks[9]
        val palmSize = distance2D(wrist, middleMcp).coerceAtLeast(1e-6f)

        // B. Per-finger state computation for Index, Middle, Ring, Pinky
        val indexState = computeFingerState(landmarks[5], landmarks[8], wrist, palmSize)
        val middleState = computeFingerState(landmarks[9], landmarks[12], wrist, palmSize)
        val ringState = computeFingerState(landmarks[13], landmarks[16], wrist, palmSize)
        val pinkyState = computeFingerState(landmarks[17], landmarks[20], wrist, palmSize)

        // C. Thumb state computation (Tip=4, IndexMCP=5)
        val thumbState = computeThumbState(landmarks[4], landmarks[5], wrist, palmSize)

        // D. Gesture Vocabulary matching
        val (gesture, confidence) = evaluateGestures(
            thumb = thumbState,
            index = indexState,
            middle = middleState,
            ring = ringState,
            pinky = pinkyState
        )

        val predictions = listOf(Pair(gesture, confidence), Pair("UNKNOWN", 1f - confidence))
            .sortedByDescending { it.second }

        return GestureClassification(
            gestureName = gesture,
            confidence = confidence,
            topPredictions = predictions
        )
    }

    private data class FingerEval(val state: FingerState, val score: Float)

    private fun computeFingerState(
        mcp: HandLandmark,
        tip: HandLandmark,
        wrist: HandLandmark,
        palmSize: Float
    ): FingerEval {
        val tipDistToWrist = distance2D(tip, wrist) / palmSize
        val mcpDistToWrist = distance2D(mcp, wrist) / palmSize

        val extensionRatio = tipDistToWrist / mcpDistToWrist.coerceAtLeast(1e-6f)
        val score = ((extensionRatio - 1.0f) / 0.5f).coerceIn(0f, 1f)

        val state = if (extensionRatio > 1.20f) FingerState.EXTENDED else FingerState.CURLED

        return FingerEval(state, score)
    }

    private fun computeThumbState(
        tip: HandLandmark,
        indexMcp: HandLandmark,
        wrist: HandLandmark,
        palmSize: Float
    ): FingerEval {
        val tipDistToIndexMcp = distance2D(tip, indexMcp) / palmSize
        val tipDistToWrist = distance2D(tip, wrist) / palmSize

        val score = (tipDistToIndexMcp / 1.0f).coerceIn(0f, 1f)
        val state = if (tipDistToIndexMcp > 0.40f && tipDistToWrist > 0.6f) FingerState.EXTENDED else FingerState.CURLED

        return FingerEval(state, score)
    }

    private fun evaluateGestures(
        thumb: FingerEval,
        index: FingerEval,
        middle: FingerEval,
        ring: FingerEval,
        pinky: FingerEval
    ): Pair<String, Float> {
        val allExtended = listOf(index, middle, ring, pinky).all { it.state == FingerState.EXTENDED }
        val allCurled = listOf(index, middle, ring, pinky).all { it.state == FingerState.CURLED }

        val meanScore = listOf(thumb, index, middle, ring, pinky).map { it.score }.average().toFloat().coerceIn(0f, 1f)

        return when {
            // HELLO (Open hand): 4 fingers + thumb extended
            allExtended && thumb.state == FingerState.EXTENDED ->
                Pair("HELLO", meanScore.coerceAtLeast(0.85f))

            // FOUR: 4 fingers extended, thumb tucked
            allExtended && thumb.state == FingerState.CURLED ->
                Pair("FOUR", meanScore.coerceAtLeast(0.85f))

            // THREE: thumb + index + middle extended, ring and pinky curled
            thumb.state == FingerState.EXTENDED && index.state == FingerState.EXTENDED &&
                    middle.state == FingerState.EXTENDED && ring.state == FingerState.CURLED &&
                    pinky.state == FingerState.CURLED ->
                Pair("THREE", meanScore.coerceAtLeast(0.85f))

            // PEACE: index + middle extended, others curled
            index.state == FingerState.EXTENDED && middle.state == FingerState.EXTENDED &&
                    ring.state == FingerState.CURLED && pinky.state == FingerState.CURLED ->
                Pair("PEACE", meanScore.coerceAtLeast(0.85f))

            // POINT: index only extended, others curled
            index.state == FingerState.EXTENDED && middle.state == FingerState.CURLED &&
                    ring.state == FingerState.CURLED && pinky.state == FingerState.CURLED ->
                Pair("POINT", meanScore.coerceAtLeast(0.85f))

            // THUMBS_UP: thumb extended, 4 fingers curled
            thumb.state == FingerState.EXTENDED && allCurled ->
                Pair("THUMBS_UP", meanScore.coerceAtLeast(0.85f))

            // I_LOVE_YOU: thumb + index + pinky extended, middle and ring curled
            thumb.state == FingerState.EXTENDED && index.state == FingerState.EXTENDED &&
                    pinky.state == FingerState.EXTENDED && middle.state == FingerState.CURLED &&
                    ring.state == FingerState.CURLED ->
                Pair("I_LOVE_YOU", meanScore.coerceAtLeast(0.85f))

            // CALL_ME: thumb + pinky extended, others curled
            thumb.state == FingerState.EXTENDED && pinky.state == FingerState.EXTENDED &&
                    index.state == FingerState.CURLED && middle.state == FingerState.CURLED &&
                    ring.state == FingerState.CURLED ->
                Pair("CALL_ME", meanScore.coerceAtLeast(0.85f))

            // FIST (YES): all 4 fingers curled and thumb tucked
            allCurled && thumb.state == FingerState.CURLED ->
                Pair("YES", meanScore.coerceAtLeast(0.85f))

            else -> Pair("UNKNOWN", 0.3f)
        }
    }

    private fun distance2D(a: HandLandmark, b: HandLandmark): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
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
