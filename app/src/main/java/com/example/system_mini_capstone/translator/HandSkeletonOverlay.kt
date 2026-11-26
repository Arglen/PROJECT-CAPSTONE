package com.example.system_mini_capstone.translator

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.example.system_mini_capstone.ml.HandResult
import kotlin.math.max

/**
 * Renders 21-node MediaPipe hand skeletons for multiple detected hands over the camera preview,
 * with distinguishable colors (Left hand Blue #246BFD, Right hand Green #0F8A6D) and wrist labels,
 * compensating for PreviewView FILL_CENTER scale type and lens mirroring.
 */
@Composable
fun HandSkeletonOverlay(
    hands: List<HandResult>,
    isMirrored: Boolean,
    modifier: Modifier = Modifier
) {
    if (hands.isEmpty()) return

    Canvas(modifier = modifier) {
        val viewW = size.width
        val viewH = size.height
        if (viewW <= 0f || viewH <= 0f) return@Canvas

        val imgW = 720f
        val imgH = 1280f

        val scaleX = viewW / imgW
        val scaleY = viewH / imgH
        val scale = max(scaleX, scaleY)

        val scaledW = imgW * scale
        val scaledH = imgH * scale

        val offsetX = (viewW - scaledW) / 2f
        val offsetY = (viewH - scaledH) / 2f

        val connections = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4,       // Thumb
            0 to 5, 5 to 6, 6 to 7, 7 to 8,       // Index
            5 to 9, 9 to 10, 10 to 11, 11 to 12,  // Middle
            9 to 13, 13 to 14, 14 to 15, 15 to 16,// Ring
            13 to 17, 17 to 18, 18 to 19, 19 to 20, // Pinky
            0 to 17                               // Palm base
        )

        for (hand in hands) {
            val landmarks = hand.landmarks
            if (landmarks.size < 21) continue

            val isLeft = hand.handedness.equals("Left", ignoreCase = true)
            val lineColor = if (isLeft) Color(0xFF246BFD) else Color(0xFF0F8A6D)
            val jointColor = if (isLeft) Color(0xFF93C5FD) else Color(0xFF6EE7B7)

            for (conn in connections) {
                val p1 = landmarks[conn.first]
                val p2 = landmarks[conn.second]

                val x1 = if (isMirrored) (1f - p1.x) else p1.x
                val x2 = if (isMirrored) (1f - p2.x) else p2.x

                val screenX1 = x1 * scaledW + offsetX
                val screenY1 = p1.y * scaledH + offsetY
                val screenX2 = x2 * scaledW + offsetX
                val screenY2 = p2.y * scaledH + offsetY

                drawLine(
                    color = lineColor,
                    start = Offset(screenX1, screenY1),
                    end = Offset(screenX2, screenY2),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }

            for (lm in landmarks) {
                val x = if (isMirrored) (1f - lm.x) else lm.x
                val screenX = x * scaledW + offsetX
                val screenY = lm.y * scaledH + offsetY

                drawCircle(
                    color = jointColor,
                    radius = 5f,
                    center = Offset(screenX, screenY)
                )
            }
        }
    }
}
