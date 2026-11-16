package com.example.system_mini_capstone

import com.example.system_mini_capstone.admin.studio.SignSampleNormalizer
import com.example.system_mini_capstone.admin.studio.SignSampleValidator
import com.example.system_mini_capstone.ml.HandLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignSampleStudioTest {

    @Test
    fun testLabelValidation() {
        assertTrue(SignSampleValidator.isValidLabel("HELLO"))
        assertTrue(SignSampleValidator.isValidLabel("A"))
        assertTrue(SignSampleValidator.isValidLabel("SIGN_1"))
        assertFalse(SignSampleValidator.isValidLabel(""))
        assertFalse(SignSampleValidator.isValidLabel("hello lowercase"))
        assertFalse(SignSampleValidator.isValidLabel("TOOLONG_LABEL_EXCEEDING_TWENTY"))
        assertFalse(SignSampleValidator.isValidLabel("SPECIAL!"))
    }

    @Test
    fun testLandmarkNormalizationInvariance() {
        val landmarks1 = List(21) { i -> HandLandmark(i.toFloat(), i.toFloat() * 2f, i.toFloat() * 0.5f) }
        val normalized1 = SignSampleNormalizer.normalize(landmarks1)

        // Translate all landmarks by offset (5, 5, 5)
        val landmarks2 = landmarks1.map { lm -> HandLandmark(lm.x + 5f, lm.y + 5f, lm.z + 5f) }
        val normalized2 = SignSampleNormalizer.normalize(landmarks2)

        assertEquals(63, normalized1.size)
        assertEquals(63, normalized2.size)

        for (i in normalized1.indices) {
            assertEquals(normalized1[i], normalized2[i], 1e-4f)
        }
    }
}
