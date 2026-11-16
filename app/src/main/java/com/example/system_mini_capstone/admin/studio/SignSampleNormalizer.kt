package com.example.system_mini_capstone.admin.studio

import com.example.system_mini_capstone.ml.HandLandmark
import kotlin.math.sqrt

/**
 * Normalizes 21 raw hand landmarks into 63 invariant float coordinates.
 * Convention:
 * 1. Subtract the wrist (landmark 0) from every landmark point (translation invariance).
 * 2. Divide all coordinates by the 3D Euclidean distance between wrist (0) and middle finger MCP (9) (scale invariance).
 * 3. If front camera is active, landmarks are pre-mirrored in HandLandmarkerHelper to match Translator preview convention.
 */
object SignSampleNormalizer {

    fun normalize(landmarks: List<HandLandmark>): List<Float> {
        if (landmarks.size < 21) return emptyList()

        val wrist = landmarks[0]
        val middleMcp = landmarks[9]

        // 1. Translate relative to wrist (landmark 0)
        val translated = landmarks.map { lm ->
            Triple(lm.x - wrist.x, lm.y - wrist.y, lm.z - wrist.z)
        }

        // 2. Compute 3D distance between wrist (0) and middle finger MCP (9)
        val t9 = translated[9]
        val distance = sqrt((t9.first * t9.first) + (t9.second * t9.second) + (t9.third * t9.third))

        val scale = if (distance > 0.0001f) distance else 1.0f

        // 3. Scale and flatten into 63 floats
        val flattened = mutableListOf<Float>()
        for (pt in translated) {
            flattened.add(pt.first / scale)
            flattened.add(pt.second / scale)
            flattened.add(pt.third / scale)
        }

        return flattened
    }
}
