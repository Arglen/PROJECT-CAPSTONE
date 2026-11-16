package com.example.system_mini_capstone.admin.studio

/**
 * Validates sign sample labels.
 * Rules: trimmed, uppercase, 1-20 characters, letters, numbers, and underscores only.
 */
object SignSampleValidator {
    private val LABEL_REGEX = Regex("^[A-Z0-9_]{1,20}$")

    fun isValidLabel(label: String): Boolean {
        val trimmed = label.trim().uppercase()
        return LABEL_REGEX.matches(trimmed)
    }
}
