package com.example.golfinv.utils

import android.graphics.Bitmap

object ImageUtils {

    /**
     * Crops the captured camera bitmap to match the yellow reticle frame box
     * positioned at the center of the camera screen.
     */
    fun cropToYellowReticleBox(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Reticle box is 85% width, and ~45% height centered
        val boxWidth = (width * 0.85f).toInt()
        val boxHeight = (height * 0.45f).toInt()

        val left = ((width - boxWidth) / 2).coerceAtLeast(0)
        val top = ((height - boxHeight) / 2).coerceAtLeast(0)

        val safeWidth = boxWidth.coerceAtMost(width - left)
        val safeHeight = boxHeight.coerceAtMost(height - top)

        return try {
            Bitmap.createBitmap(bitmap, left, top, safeWidth, safeHeight)
        } catch (e: Exception) {
            bitmap // Fallback to original bitmap if crop fails
        }
    }
}
