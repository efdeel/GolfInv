package com.example.golfinv.recognition

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class IdentifiedGolfClub(
    val brand: String,
    val clubType: String = "Driver",
    val modelType: String,
    val loft: String,
    val isAdjustable: Boolean,
    val releaseYear: String,
    val confidence: Float = 0.8f,
    val rawOcrText: String = "",
    val isWebVerified: Boolean = false,
    val webSourceDescription: String = "",
    val hasScrewDetected: Boolean = false,
    val screwMessage: String = "",
    val visualMatchScore: Float = 0.96f,

    // Side-by-Side comparison fields
    val ocrBrand: String = "",
    val ocrModel: String = "",
    val ocrLoft: String = "10.5°",
    val ocrYear: String = "2024",

    val webBrand: String = "",
    val webModel: String = "",
    val webLoft: String = "10.5°",
    val webYear: String = "2024",
    val webUrl: String = ""
)

object GolfClubRecognitionEngine {

    val clubTypeOptions = listOf("Driver", "Fairway Wood", "Rescue", "Iron", "Wedge", "Putter")

    // Objective, unbiased catalog of golf club head specifications
    private val catalog = listOf(
        ClubCatalogEntry(listOf("QI10", "MAX"), "TaylorMade", "Driver", "Qi10 Max Driver", "10.5°", true, "2024"),
        ClubCatalogEntry(listOf("QI10", "LS"), "TaylorMade", "Driver", "Qi10 LS Driver", "9.0°", true, "2024"),
        ClubCatalogEntry(listOf("QI10"), "TaylorMade", "Driver", "Qi10 Driver", "10.5°", true, "2024"),
        ClubCatalogEntry(listOf("STEALTH", "2", "HD"), "TaylorMade", "Driver", "Stealth 2 HD Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("STEALTH", "2"), "TaylorMade", "Driver", "Stealth 2 Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("STEALTH", "PLUS"), "TaylorMade", "Driver", "Stealth Plus Driver", "9.0°", true, "2022"),
        ClubCatalogEntry(listOf("STEALTH"), "TaylorMade", "Driver", "Stealth Driver", "10.5°", true, "2022"),
        ClubCatalogEntry(listOf("SIM2", "MAX"), "TaylorMade", "Driver", "SIM2 Max Driver", "10.5°", true, "2021"),
        ClubCatalogEntry(listOf("SIM2"), "TaylorMade", "Driver", "SIM2 Driver", "9.0°", true, "2021"),

        ClubCatalogEntry(listOf("TITLEIST", "GT2"), "Titleist", "Driver", "GT2 Driver", "10.0°", true, "2024"),
        ClubCatalogEntry(listOf("TITLEIST", "GT3"), "Titleist", "Driver", "GT3 Driver", "9.0°", true, "2024"),
        ClubCatalogEntry(listOf("TSR2"), "Titleist", "Driver", "TSR2 Driver", "10.0°", true, "2022"),
        ClubCatalogEntry(listOf("TSR3"), "Titleist", "Driver", "TSR3 Driver", "9.0°", true, "2022"),

        ClubCatalogEntry(listOf("MAVRIK", "SUB", "ZERO"), "Callaway", "Driver", "Callaway Mavrik Sub Zero Driver", "9.0°", true, "2020"),
        ClubCatalogEntry(listOf("MAVRIK", "MAX"), "Callaway", "Driver", "Callaway Mavrik Max Driver", "10.5°", true, "2020"),
        ClubCatalogEntry(listOf("MAVRIK"), "Callaway", "Driver", "Callaway Mavrik Driver", "10.5°", true, "2020"),
        ClubCatalogEntry(listOf("PARADYM", "AI", "SMOKE"), "Callaway", "Driver", "Paradym Ai Smoke MAX Driver", "10.5°", true, "2024"),
        ClubCatalogEntry(listOf("PARADYM"), "Callaway", "Driver", "Paradym Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("ROGUE", "ST"), "Callaway", "Driver", "Rogue ST MAX Driver", "10.5°", true, "2022"),
        ClubCatalogEntry(listOf("EPIC"), "Callaway", "Driver", "Epic Speed Driver", "10.5°", true, "2021"),

        ClubCatalogEntry(listOf("G430", "MAX"), "PING", "Driver", "G430 MAX Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("G430"), "PING", "Driver", "G430 Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("G425"), "PING", "Driver", "G425 Driver", "9.0°", true, "2020"),

        ClubCatalogEntry(listOf("DARKSPEED"), "Cobra", "Driver", "Darkspeed MAX Driver", "10.5°", true, "2024"),
        ClubCatalogEntry(listOf("AEROJET"), "Cobra", "Driver", "Aerojet Driver", "10.5°", true, "2023"),

        ClubCatalogEntry(listOf("ST-MAX", "230"), "Mizuno", "Driver", "ST-MAX 230 Driver", "9.5°", true, "2024")
    )

    private val defaultBrands = listOf(
        "Titleist", "Callaway", "TaylorMade", "PING", "Cobra Golf", "PXG", "Wilson Staff", "Cleveland Golf",
        "Bettinardi Golf", "Odyssey", "Scotty Cameron", "Tour Edge", "Sub 70", "Maltby Golf", "New Level Golf",
        "Ben Hogan Golf", "MacGregor Golf", "Adams Golf", "PowerBilt", "Orlimar", "Krank Golf", "L.A.B. Golf",
        "Evnroll", "T.P. Mills", "KZG", "STX Golf", "Avoda Golf", "Vertical Groove Golf", "1 Iron Golf",
        "Kirkland Signature", "Vokey Design", "Kronos Golf", "Bobby Jones Golf", "Nickent Golf", "Tommy Armour Golf",
        "Golden Bear", "Bionik", "Axis1", "SeeMore Putters", "Artisan Golf", "Piretti", "Tyson Lamb", "Machine Golf",
        "Byron Morgan", "MannKrafted", "Xenon Golf", "Lajosi", "SIK Golf", "Rife Putters", "Mizuno", "Honma Golf",
        "Srixon", "XXIO", "Bridgestone Golf", "Yamaha Golf", "PRGR", "Yonex", "ONOFF", "Majesty Golf", "Maruman",
        "Fourteen Golf", "Epon Golf", "Miura Golf", "Miura Giken", "BALDO", "RomaRo", "Roddio", "JBEAM", "KAMUI",
        "KAMUI WORKS", "Ryoma Golf", "Geotech Golf", "Royal Collection", "Akira Products", "Masda Golf", "A Design Golf",
        "Jucie", "Zodia", "Modart", "Fuso Dream", "Crazy Golf", "Muziik", "GTD Golf", "FREIHEIT", "MYSTERY", "Kasco",
        "Roots Golf", "Katana Golf", "JIGEN", "GINNICO", "Grandista", "DOCUS", "WAOWW", "Progress Golf", "George Spirits",
        "Kyoei Golf", "Fujimoto Gikoh", "ENA Golf", "S-YARD", "WORKS GOLF", "A.F.D.", "EMILLID BAHAMA", "Jean-Baptiste",
        "PROTOCONCEPT", "Maruman Majesty", "Masamune Golf", "GOLF Partner Original", "KAMUI Pro", "Ben Sayers",
        "Forgan of St Andrews", "John Letters", "Slazenger", "Benross", "Masters Golf", "Lynx Golf", "Inesis",
        "Kramski", "BIG MAX", "Longridge", "GolfKicks", "PGM (Pro Golf Master)", "Koala Golf", "Handa Golf",
        "AG High Performance", "Shenzhen Impact Golf", "3Bays"
    )

    suspend fun processImage(croppedBitmap: Bitmap, customBrands: List<String> = emptyList()): IdentifiedGolfClub {
        // 1. Perform OCR objectively
        val ocrIdentified = runOcrOnBitmap(croppedBitmap, customBrands)

        // 2. Perform Screw Detection
        val screwResult = ScrewDetector.detectScrewOnGolfHead(croppedBitmap, ocrIdentified.rawOcrText)

        // 3. Perform Web Search & Visual Match
        val webResult = GolfWebSearchVerifier.verifyAndMatchWeb(
            detectedBrand = ocrIdentified.brand,
            detectedModel = ocrIdentified.modelType,
            rawOcrText = ocrIdentified.rawOcrText
        )

        val visualResult = GolfVisualWebSearchEngine.performVisualWebSearch(
            croppedBitmap = croppedBitmap,
            ocrText = ocrIdentified.rawOcrText,
            initialBrand = ocrIdentified.brand,
            initialModel = ocrIdentified.modelType
        )

        val finalAdjustable = screwResult.hasScrew || webResult.isAdjustable == true || ocrIdentified.isAdjustable

        val webBrand = visualResult.brand ?: webResult.brand ?: ocrIdentified.brand
        val webModel = visualResult.modelType ?: webResult.modelType ?: ocrIdentified.modelType
        val webLoft = visualResult.loft ?: webResult.loft ?: ocrIdentified.loft
        val webYear = visualResult.releaseYear ?: webResult.releaseYear ?: ocrIdentified.releaseYear

        val primaryBrand = if (visualResult.isMatched) webBrand else ocrIdentified.brand
        val primaryModel = if (visualResult.isMatched) webModel else ocrIdentified.modelType
        val primaryLoft = if (visualResult.isMatched) webLoft else ocrIdentified.loft
        val primaryYear = if (visualResult.isMatched) webYear else ocrIdentified.releaseYear

        val detectedClubType = detectClubTypeFromText(primaryModel, ocrIdentified.rawOcrText)

        return IdentifiedGolfClub(
            brand = primaryBrand,
            clubType = detectedClubType,
            modelType = primaryModel,
            loft = primaryLoft,
            isAdjustable = finalAdjustable,
            releaseYear = primaryYear,
            confidence = 0.95f,
            rawOcrText = ocrIdentified.rawOcrText,
            isWebVerified = visualResult.isMatched,
            webSourceDescription = visualResult.webSourceTitle,
            hasScrewDetected = screwResult.hasScrew || finalAdjustable,
            screwMessage = screwResult.detailMessage.ifBlank { "Baut Sleeve Hosel & Pemberat Terdeteksi" },
            visualMatchScore = visualResult.similarityScore,

            // Left column (OCR Data)
            ocrBrand = ocrIdentified.brand,
            ocrModel = ocrIdentified.modelType,
            ocrLoft = ocrIdentified.loft,
            ocrYear = ocrIdentified.releaseYear,

            // Right column (Web Search Data)
            webBrand = webBrand,
            webModel = webModel,
            webLoft = webLoft,
            webYear = webYear,
            webUrl = visualResult.webSourceUrl
        )
    }

    private suspend fun runOcrOnBitmap(bitmap: Bitmap, customBrands: List<String>): IdentifiedGolfClub = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { text ->
                val rawText = text.text.uppercase(Locale.ROOT)
                val result = identifyFromText(rawText, customBrands)
                continuation.resume(result)
            }
            .addOnFailureListener {
                continuation.resume(
                    IdentifiedGolfClub(
                        brand = "No Brand",
                        clubType = "Driver",
                        modelType = "Driver / Golf Club",
                        loft = "10.5°",
                        isAdjustable = true,
                        releaseYear = "2024",
                        confidence = 0.3f,
                        rawOcrText = ""
                    )
                )
            }
    }

    private fun identifyFromText(ocrText: String, customBrands: List<String>): IdentifiedGolfClub {
        // Objective evaluation of catalog entries based strictly on keyword match count
        var bestCatalogMatch: ClubCatalogEntry? = null
        var maxMatchedKeywords = 0

        for (entry in catalog) {
            val matchedCount = entry.keywords.count { kw -> ocrText.contains(kw) }
            if (matchedCount > maxMatchedKeywords) {
                maxMatchedKeywords = matchedCount
                bestCatalogMatch = entry
            }
        }

        val hasBoltOrSleeve = ocrText.contains("BAUT") ||
                ocrText.contains("SCREW") ||
                ocrText.contains("BOLT") ||
                ocrText.contains("WEIGHT") ||
                ocrText.contains("SLEEVE") ||
                ocrText.contains("ADJUSTABLE") ||
                ocrText.contains("MAX") ||
                ocrText.contains("DRIVER")

        // Only match catalog if keywords match objectively
        if (bestCatalogMatch != null && maxMatchedKeywords >= 2) {
            val detectedLoft = extractLoft(ocrText) ?: bestCatalogMatch.defaultLoft
            return IdentifiedGolfClub(
                brand = bestCatalogMatch.brand,
                clubType = bestCatalogMatch.clubType,
                modelType = bestCatalogMatch.modelType,
                loft = detectedLoft,
                isAdjustable = if (hasBoltOrSleeve) true else bestCatalogMatch.isAdjustable,
                releaseYear = bestCatalogMatch.releaseYear,
                confidence = 0.92f,
                rawOcrText = ocrText
            )
        }

        val activeBrands = customBrands.ifEmpty { defaultBrands }
        val brand = findBestFuzzyBrandMatch(ocrText, activeBrands)
        val detectedLoft = extractLoft(ocrText) ?: "10.5°"

        val modelWords = ocrText.split("\\s+".toRegex())
            .filter { word -> word.length > 2 && !activeBrands.any { it.uppercase(Locale.ROOT) == word } }
            .take(3)
            .joinToString(" ")

        val modelType = if (modelWords.isNotBlank()) modelWords else "Driver / Club Head"
        val yearMatch = Regex("""\b(201[5-9]|202[0-5])\b""").find(ocrText)?.value ?: "2024"
        val clubType = detectClubTypeFromText(modelType, ocrText)

        return IdentifiedGolfClub(
            brand = brand,
            clubType = clubType,
            modelType = modelType,
            loft = detectedLoft,
            isAdjustable = hasBoltOrSleeve,
            releaseYear = yearMatch,
            confidence = 0.6f,
            rawOcrText = ocrText
        )
    }

    private fun findBestFuzzyBrandMatch(ocrText: String, activeBrands: List<String>): String {
        val upperOcr = ocrText.uppercase(Locale.ROOT)
        val ocrWords = upperOcr.split(Regex("\\s+"))

        var bestMatchBrand = "No Brand"
        var highestScore = 0.0

        for (brand in activeBrands) {
            val upperBrand = brand.uppercase(Locale.ROOT)
            if (upperOcr.contains(upperBrand)) return brand

            for (word in ocrWords) {
                if (word.length < 3) continue
                val score = calculateCharacterSimilarity(word, upperBrand)
                if (score > highestScore) {
                    highestScore = score
                    bestMatchBrand = brand
                }
            }

            val fullScore = calculateCharacterSimilarity(upperOcr.replace(Regex("\\s+"), ""), upperBrand.replace(Regex("\\s+"), ""))
            if (fullScore > highestScore) {
                highestScore = fullScore
                bestMatchBrand = brand
            }
        }
        return if (highestScore >= 0.65) bestMatchBrand else "No Brand"
    }

    private fun calculateCharacterSimilarity(s1: String, s2: String): Double {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val shorter = if (s1.length < s2.length) s1 else s2
        val longer = if (s1.length >= s2.length) s1 else s2

        var matches = 0
        val shorterChars = shorter.toMutableList()
        for (char in longer) {
            if (shorterChars.remove(char)) {
                matches++
            }
        }
        return (2.0 * matches) / (s1.length + s2.length)
    }

    private fun detectClubTypeFromText(model: String, text: String): String {
        val upper = "$model $text".uppercase(Locale.ROOT)
        return when {
            upper.contains("WOOD") || upper.contains("3W") || upper.contains("5W") || upper.contains("FAIRWAY") -> "Fairway Wood"
            upper.contains("RESCUE") || upper.contains("HYBRID") || upper.contains("UTILITY") -> "Rescue"
            upper.contains("IRON") || upper.contains("FORGED") || upper.contains("P790") || upper.contains("T100") || upper.contains("JPX") -> "Iron"
            upper.contains("WEDGE") || upper.contains("SM9") || upper.contains("SM10") || upper.contains("JAWS") || upper.contains("RTX") || upper.contains("54°") || upper.contains("56°") || upper.contains("60°") -> "Wedge"
            upper.contains("PUTTER") || upper.contains("SCOTTY") || upper.contains("ODYSSEY") || upper.contains("PHANTOM") -> "Putter"
            else -> "Driver"
        }
    }

    private fun extractLoft(text: String): String? {
        val loftRegex = Regex("""\b(8\.0|8\.5|9\.0|9\.5|10\.0|10\.5|11\.0|11\.5|12\.0|13\.5|15\.0|16\.5|18\.0|19\.0|21\.0|24\.0|52|54|56|58|60)°?\b""")
        val match = loftRegex.find(text)
        return match?.value?.let { if (it.endsWith("°")) it else "$it°" }
    }
}

private data class ClubCatalogEntry(
    val keywords: List<String>,
    val brand: String,
    val clubType: String,
    val modelType: String,
    val defaultLoft: String,
    val isAdjustable: Boolean,
    val releaseYear: String
)
