package com.example.system_mini_capstone

import com.example.system_mini_capstone.ml.HandLandmark
import com.example.system_mini_capstone.ml.SignClassifierEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SignClassifierEngineTest {

    @Test
    fun testEmptyLandmarksClassification() {
        val engine = SignClassifierEngine()
        val result = engine.classifyGesture(emptyList())
        assertEquals("NONE", result.gestureName)
        assertEquals(0f, result.confidence)
        engine.close()
    }

    @Test
    fun testHeuristicGestureClassificationOpenHand() {
        val engine = SignClassifierEngine()

        // Create 21 hand landmarks simulating an open hand (HELLO sign)
        val landmarks = mutableListOf<HandLandmark>()
        // Wrist at 0,0,0
        landmarks.add(HandLandmark(0f, 0f, 0f))
        for (i in 1..20) {
            // Tips further away from wrist
            val distFactor = if (i % 4 == 0) 2.0f else 1.0f
            landmarks.add(HandLandmark(i * 0.1f * distFactor, i * 0.1f * distFactor, 0f))
        }

        val result = engine.classifyGesture(landmarks)
        assertNotNull(result.gestureName)
        assertNotNull(result.confidence)
        engine.close()
    }
}
