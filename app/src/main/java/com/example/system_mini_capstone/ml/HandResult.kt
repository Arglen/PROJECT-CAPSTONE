package com.example.system_mini_capstone.ml

data class HandResult(
    val handedness: String, // "Left" or "Right" (logical signer perspective)
    val score: Float,
    val landmarks: List<HandLandmark>
)

/**
 * Maps MediaPipe raw handedness label to the signer's real logical left/right hand
 * depending on whether the active lens is the front camera (mirrored selfie view)
 * or back camera (unmirrored world view).
 *
 * Input orientation convention:
 * - Front camera: Input bitmap is flipped horizontally (mirror view). MediaPipe sees left as right.
 * - Back camera: Input bitmap is unmirrored. MediaPipe sees left as left.
 */
fun toLogicalHandedness(rawLabel: String, isFrontCamera: Boolean, score: Float): String {
    val isRawLeft = rawLabel.equals("Left", ignoreCase = true)
    return if (isFrontCamera) {
        if (isRawLeft) "Right" else "Left"
    } else {
        if (isRawLeft) "Left" else "Right"
    }
}
