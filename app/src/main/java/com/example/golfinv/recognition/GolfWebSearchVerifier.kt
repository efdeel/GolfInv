package com.example.golfinv.recognition

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.regex.Pattern

data class WebVerificationResult(
    val isVerified: Boolean,
    val brand: String? = null,
    val modelType: String? = null,
    val loft: String? = null,
    val isAdjustable: Boolean? = null,
    val releaseYear: String? = null,
    val sourceDescription: String = ""
)

object GolfWebSearchVerifier {

    private const val TAG = "GolfWebSearchVerifier"

    suspend fun verifyAndMatchWeb(
        detectedBrand: String,
        detectedModel: String,
        rawOcrText: String
    ): WebVerificationResult = withContext(Dispatchers.IO) {
        try {
            val queryText = "$detectedBrand $detectedModel golf club specs release year bolt adjustable sleeve".trim()
            val encodedQuery = URLEncoder.encode(queryText, "UTF-8")
            val urlString = "https://html.duckduckgo.com/html/?q=$encodedQuery"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val cleanText = responseText.replace(Regex("<[^>]*>"), " ").uppercase(Locale.ROOT)

                // Verify brand match on web
                val brandUpper = detectedBrand.uppercase(Locale.ROOT)
                val modelUpper = detectedModel.uppercase(Locale.ROOT)

                val hasBrandInWeb = cleanText.contains(brandUpper) || brandUpper.split(" ").any { it.length > 3 && cleanText.contains(it) }
                val hasModelInWeb = cleanText.contains(modelUpper) || modelUpper.split(" ").any { it.length > 3 && cleanText.contains(it) }

                if (hasBrandInWeb || hasModelInWeb) {
                    // Extract release year from web search snippets
                    val yearMatcher = Pattern.compile("""\b(201[5-9]|202[0-5])\b""").matcher(cleanText)
                    val webYear = if (yearMatcher.find()) yearMatcher.group(1) else null

                    // Detection of Bolt / Screw / Sleeve on Head indicates Adjustable Loft
                    val hasBoltOrSleeveOnWeb = cleanText.contains("BAUT") ||
                            cleanText.contains("SCREW") ||
                            cleanText.contains("BOLT") ||
                            cleanText.contains("WEIGHT") ||
                            cleanText.contains("SLEEVE") ||
                            cleanText.contains("ADJUSTABLE HOSEL") ||
                            cleanText.contains("OPTIFIT") ||
                            cleanText.contains("SUREFIT") ||
                            cleanText.contains("LOFT SLEEVE") ||
                            cleanText.contains("FCT") ||
                            cleanText.contains("MYFLY") ||
                            cleanText.contains("TRAJECTORY TUNING")

                    val hasFixedOnWeb = cleanText.contains("FIXED HOSEL") || cleanText.contains("BONDED HOSEL")
                    val isAdjustableWeb = if (hasBoltOrSleeveOnWeb) true else if (hasFixedOnWeb) false else true

                    // Extract loft specs from web description
                    val loftMatcher = Pattern.compile("""\b(8\.5|9\.0|9\.5|10\.0|10\.5|11\.5|12\.0|15\.0|18\.0|52|54|56|60)°?\b""").matcher(cleanText)
                    val webLoft = if (loftMatcher.find()) {
                        val matched = loftMatcher.group(1)
                        if (matched != null && !matched.endsWith("°")) "$matched°" else matched
                    } else null

                    val sourceMsg = if (hasBoltOrSleeveOnWeb)
                        "Terverifikasi Web ($detectedBrand $detectedModel - Terdeteksi Baut/Sleeve Adjustable)"
                    else
                        "Terverifikasi Web ($detectedBrand $detectedModel)"

                    Log.d(TAG, "Web verification successful for $detectedBrand $detectedModel. Year: $webYear, Sleeve/Bolt: $isAdjustableWeb")

                    return@withContext WebVerificationResult(
                        isVerified = true,
                        brand = detectedBrand,
                        modelType = detectedModel,
                        loft = webLoft,
                        isAdjustable = isAdjustableWeb,
                        releaseYear = webYear,
                        sourceDescription = sourceMsg
                    )
                }
            }

            return@withContext WebVerificationResult(
                isVerified = false,
                sourceDescription = "Pencarian Web tidak menemukan kecocokan pasti."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to web for verification", e)
            return@withContext WebVerificationResult(
                isVerified = false,
                sourceDescription = "Mode Offline / Tanpa Koneksi Web"
            )
        }
    }
}
