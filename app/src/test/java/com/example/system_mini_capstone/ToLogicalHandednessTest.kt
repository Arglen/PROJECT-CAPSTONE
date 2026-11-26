package com.example.system_mini_capstone

import com.example.system_mini_capstone.admin.studio.SignSampleNormalizer
import com.example.system_mini_capstone.ml.HandLandmark
import com.example.system_mini_capstone.ml.toLogicalHandedness
import org.junit.Assert.assertEquals
import org.junit.Test

class ToLogicalHandednessTest {

    @Test
    fun testToLogicalHandedness_FrontCamera() {
        // Front camera (mirrored selfie view): MediaPipe "Left" is signer's "Right", MediaPipe "Right" is signer's "Left"
        assertEquals("Right", toLogicalHandedness("Left", isFrontCamera = true, score = 0.95f))
        assertEquals("Left", toLogicalHandedness("Right", isFrontCamera = true, score = 0.95f))
    }

    @Test
    fun testToLogicalHandedness_BackCamera() {
        // Back camera (unmirrored world view): MediaPipe "Left" is signer's "Left", MediaPipe "Right" is signer's "Right"
        assertEquals("Left", toLogicalHandedness("Left", isFrontCamera = false, score = 0.95f))
        assertEquals("Right", toLogicalHandedness("Right", isFrontCamera = false, score = 0.95f))
    }

    @Test
    fun testCanonicalNormalization_LeftAndRightHandSymmetry() {
        val landmarksRight = List(21) { i -> HandLandmark(0.2f + i * 0.01f, 0.3f + i * 0.01f, 0f) }
        val landmarksLeftMirrored = landmarksRight.map { lm -> HandLandmark(1f - lm.x, lm.y, lm.z) }

        val normRight = SignSampleNormalizer.normalize(landmarksRight, "Right")
        val normLeft = SignSampleNormalizer.normalize(landmarksLeftMirrored, "Left")

        assertEquals(normRight.size, normLeft.size)
        for (i in normRight.indices) {
            assertEquals(normRight[i], normLeft[i], 1e-4f)
        }
    }
}
