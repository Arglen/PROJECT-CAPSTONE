package com.example.system_mini_capstone

import com.example.system_mini_capstone.ml.HandLandmark
import com.example.system_mini_capstone.ml.SignClassifierEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SignClassifierEngineTest {

    private val engine = SignClassifierEngine()

    private fun buildMockHand(extendedFingers: List<Boolean>, thumbExtended: Boolean): List<HandLandmark> {
        val landmarks = MutableList(21) { HandLandmark(0.5f, 0.5f, 0f) }
        // Wrist at (0.5, 0.9)
        landmarks[0] = HandLandmark(0.5f, 0.9f, 0f)
        // Middle MCP (9) at (0.5, 0.6) -> Palm size = 0.3
        landmarks[9] = HandLandmark(0.5f, 0.6f, 0f)

        val fingerConfigs = listOf(
            Triple(listOf(5, 6, 7, 8), extendedFingers[0], 0.42f),
            Triple(listOf(9, 10, 11, 12), extendedFingers[1], 0.47f),
            Triple(listOf(13, 14, 15, 16), extendedFingers[2], 0.52f),
            Triple(listOf(17, 18, 19, 20), extendedFingers[3], 0.57f)
        )

        for ((indices, isExtended, x) in fingerConfigs) {
            val tipY = if (isExtended) 0.15f else 0.50f
            landmarks[indices[0]] = HandLandmark(x, 0.55f, 0f) // MCP
            landmarks[indices[1]] = HandLandmark(x, 0.45f, 0f) // PIP
            landmarks[indices[2]] = HandLandmark(x, 0.30f, 0f) // DIP
            landmarks[indices[3]] = HandLandmark(x, tipY, 0f)  // TIP
        }

        // Thumb: MCP(2), IP(3), TIP(4)
        val thumbTipX = if (thumbExtended) 0.25f else 0.48f
        val thumbTipY = if (thumbExtended) 0.45f else 0.55f
        landmarks[2] = HandLandmark(0.44f, 0.65f, 0f)
        landmarks[3] = HandLandmark(0.35f, 0.55f, 0f)
        landmarks[4] = HandLandmark(thumbTipX, thumbTipY, 0f)

        return landmarks
    }

    @Test
    fun testEmptyLandmarks() {
        val result = engine.classifyGesture(emptyList())
        assertEquals("UNKNOWN", result.gestureName)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun testOpenHandHello() {
        val landmarks = buildMockHand(listOf(true, true, true, true), true)
        val result = engine.classifyGesture(landmarks)
        assertEquals("HELLO", result.gestureName)
    }

    @Test
    fun testFistYes() {
        val landmarks = buildMockHand(listOf(false, false, false, false), false)
        val result = engine.classifyGesture(landmarks)
        assertEquals("YES", result.gestureName)
    }

    @Test
    fun testPointGesture() {
        val landmarks = buildMockHand(listOf(true, false, false, false), false)
        val result = engine.classifyGesture(landmarks)
        assertEquals("POINT", result.gestureName)
    }

    @Test
    fun testPeaceGesture() {
        val landmarks = buildMockHand(listOf(true, true, false, false), false)
        val result = engine.classifyGesture(landmarks)
        assertEquals("PEACE", result.gestureName)
    }

    @Test
    fun testHalfCurledUnknown() {
        val landmarks = buildMockHand(listOf(true, false, true, false), true)
        val result = engine.classifyGesture(landmarks)
        assertEquals("UNKNOWN", result.gestureName)
    }

    @Test
    fun testRotatedCopy() {
        val original = buildMockHand(listOf(true, true, false, false), false)
        val rotated = original.map { lm ->
            val dx = lm.x - 0.5f
            val dy = lm.y - 0.5f
            HandLandmark(0.5f - dy, 0.5f + dx, lm.z)
        }
        val result = engine.classifyGesture(rotated)
        assertEquals("PEACE", result.gestureName)
    }
}
