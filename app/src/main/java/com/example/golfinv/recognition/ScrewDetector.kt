package com.example.golfinv.recognition

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

data class ScrewDetectionResult(
    val hasScrew: Boolean,
    val confidence: Float,
    val detailMessage: String
)

object ScrewDetector {

    private const val TAG = "ScrewDetector"

    /**
     * Analyzes the captured cropped bitmap of the golf club head to detect metallic screws / hosel bolts.
     * Combines Kotlin pixel gradient pattern matching (concentric circular screw head analysis)
     * and text markings detection.
     */
    fun detectScrewOnGolfHead(bitmap: Bitmap, ocrText: String = ""): ScrewDetectionResult {
        // 1. Text-based markings check (OCR for hosel screw markings e.g. BAUT, SCREW, STD, LOWER, HIGHER, FCT, SUREFIT)
        val textResult = checkOcrTextForScrew(ocrText)
        if (textResult.hasScrew) {
            Log.d(TAG, "Screw detected via OCR text markings: ${textResult.detailMessage}")
            return textResult
        }

        // 2. Image-based Computer Vision check: Detect circular/hexagonal metallic screw head patterns
        val imageResult = detectScrewHeadPatternsInBitmap(bitmap)
        if (imageResult.hasScrew) {
            Log.d(TAG, "Screw detected via Kotlin Bitmap Circle Analysis: ${imageResult.detailMessage}")
            return imageResult
        }

        return ScrewDetectionResult(
            hasScrew = false,
            confidence = 0.2f,
            detailMessage = "Fixed Hosel (Tidak ditemukan baut pada head)"
        )
    }

    private fun checkOcrTextForScrew(ocrText: String): ScrewDetectionResult {
        val upper = ocrText.uppercase(Locale.ROOT)
        val screwKeywords = listOf(
            "BAUT", "SCREW", "BOLT", "WEIGHT", "SLIDER", "TRACK", "FCT",
            "SUREFIT", "OPTIFIT", "MYFLY", "ADJUSTABLE", "STD LOFT", "HIGHER", "LOWER", "FLAT"
        )

        val matchedKeyword = screwKeywords.find { upper.contains(it) }
        return if (matchedKeyword != null) {
            ScrewDetectionResult(
                hasScrew = true,
                confidence = 0.95f,
                detailMessage = "Baut Terdeteksi (Tanda Baut/Sleeve '$matchedKeyword' pada Head)"
            )
        } else {
            ScrewDetectionResult(hasScrew = false, confidence = 0.0f, detailMessage = "")
        }
    }

    /**
     * Scans the bitmap for circular concentric luminance gradients typical of golf club screws/bolts
     * (e.g. hosel adjustment screws and weight port bolts).
     */
    private fun detectScrewHeadPatternsInBitmap(bitmap: Bitmap): ScrewDetectionResult {
        try {
            val width = bitmap.width
            val height = bitmap.height

            if (width < 30 || height < 30) {
                return ScrewDetectionResult(false, 0f, "")
            }

            // Downsample for fast processing
            val sampleStep = (width / 120).coerceAtLeast(1)
            val sampledW = width / sampleStep
            val sampledH = height / sampleStep

            val grayPixels = IntArray(sampledW * sampledH)
            for (y in 0 until sampledH) {
                for (x in 0 until sampledW) {
                    val px = bitmap.getPixel(x * sampleStep, y * sampleStep)
                    val r = Color.red(px)
                    val g = Color.green(px)
                    val b = Color.blue(px)
                    // Luminance formula
                    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    grayPixels[y * sampledW + x] = gray
                }
            }

            var screwPatternScore = 0

            // Search for concentric contrast changes (dark center, bright ring or vice-versa)
            val radiusMin = 3
            val radiusMax = 12

            for (y in radiusMax until sampledH - radiusMax step 3) {
                for (x in radiusMax until sampledW - radiusMax step 3) {
                    val centerLum = grayPixels[y * sampledW + x]

                    for (r in radiusMin..radiusMax step 3) {
                        // Sample 8 points around circle
                        var ringLumSum = 0
                        var count = 0
                        val angles = doubleArrayOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)

                        for (angleDeg in angles) {
                            val rad = Math.toRadians(angleDeg)
                            val pxX = (x + r * Math.cos(rad)).toInt().coerceIn(0, sampledW - 1)
                            val pxY = (y + r * Math.sin(rad)).toInt().coerceIn(0, sampledH - 1)
                            ringLumSum += grayPixels[pxY * sampledW + pxX]
                            count++
                        }

                        val avgRingLum = ringLumSum / count.toFloat()
                        val contrastDiff = abs(centerLum - avgRingLum)

                        // Golf head screws typically have a sharp dark-to-light concentric contrast > 45
                        if (contrastDiff > 45) {
                            screwPatternScore++
                        }
                    }
                }
            }

            Log.d(TAG, "Screw pattern score: $screwPatternScore")

            return if (screwPatternScore >= 4) {
                ScrewDetectionResult(
                    hasScrew = true,
                    confidence = 0.88f,
                    detailMessage = "Baut Terdeteksi pada Head (Pola Baut Hosel/Weight)"
                )
            } else {
                ScrewDetectionResult(
                    hasScrew = false,
                    confidence = 0.3f,
                    detailMessage = ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting screw head pattern in bitmap", e)
            return ScrewDetectionResult(false, 0f, "")
        }
    }
}
