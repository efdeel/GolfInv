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
    val visualMatchScore: Float = 0.96f
)

object GolfClubRecognitionEngine {

    val clubTypeOptions = listOf("Driver", "Fairway Wood", "Rescue", "Iron", "Wedge", "Putter")

    // Preset catalog of known golf club head models
    private val catalog = listOf(
        // TaylorMade
        ClubCatalogEntry(listOf("TAYLORMADE", "QI10", "MAX"), "TaylorMade", "Driver", "Qi10 Max Driver", "10.5°", true, "2024"),
        ClubCatalogEntry(listOf("TAYLORMADE", "QI10", "LS"), "TaylorMade", "Driver", "Qi10 LS Driver", "9.0°", true, "2024"),
        ClubCatalogEntry(listOf("TAYLORMADE", "QI10"), "TaylorMade", "Driver", "Qi10 Driver", "10.5°", true, "2024"),
        ClubCatalogEntry(listOf("TAYLORMADE", "STEALTH", "2", "HD"), "TaylorMade", "Driver", "Stealth 2 HD Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("TAYLORMADE", "STEALTH", "2"), "TaylorMade", "Driver", "Stealth 2 Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("TAYLORMADE", "STEALTH", "PLUS"), "TaylorMade", "Driver", "Stealth Plus Driver", "9.0°", true, "2022"),
        ClubCatalogEntry(listOf("TAYLORMADE", "STEALTH"), "TaylorMade", "Driver", "Stealth Driver", "10.5°", true, "2022"),
        ClubCatalogEntry(listOf("TAYLORMADE", "SIM2", "MAX"), "TaylorMade", "Driver", "SIM2 Max Driver", "10.5°", true, "2021"),
        ClubCatalogEntry(listOf("TAYLORMADE", "SIM2"), "TaylorMade", "Driver", "SIM2 Driver", "9.0°", true, "2021"),
        ClubCatalogEntry(listOf("TAYLORMADE", "P790"), "TaylorMade", "Iron", "P790 Irons", "23.0°", false, "2023"),
        ClubCatalogEntry(listOf("TAYLORMADE", "P770"), "TaylorMade", "Iron", "P770 Irons", "25.0°", false, "2023"),

        // Titleist
        ClubCatalogEntry(listOf("TITLEIST", "GT2"), "Titleist", "Driver", "GT2 Driver", "10.0°", true, "2024"),
        ClubCatalogEntry(listOf("TITLEIST", "GT3"), "Titleist", "Driver", "GT3 Driver", "9.0°", true, "2024"),
        ClubCatalogEntry(listOf("TITLEIST", "TSR2"), "Titleist", "Driver", "TSR2 Driver", "10.0°", true, "2022"),
        ClubCatalogEntry(listOf("TITLEIST", "TSR3"), "Titleist", "Driver", "TSR3 Driver", "9.0°", true, "2022"),
        ClubCatalogEntry(listOf("TITLEIST", "T100"), "Titleist", "Iron", "T100 Irons", "27.0°", false, "2023"),
        ClubCatalogEntry(listOf("TITLEIST", "T200"), "Titleist", "Iron", "T200 Irons", "24.0°", false, "2023"),
        ClubCatalogEntry(listOf("VOKEY", "SM10"), "Titleist", "Wedge", "Vokey Design SM10 Wedge", "56.0°", false, "2024"),
        ClubCatalogEntry(listOf("VOKEY", "SM9"), "Titleist", "Wedge", "Vokey Design SM9 Wedge", "56.0°", false, "2022"),
        ClubCatalogEntry(listOf("SCOTTY", "CAMERON"), "Titleist", "Putter", "Scotty Cameron Putter", "3.5°", false, "2024"),

        // Callaway
        ClubCatalogEntry(listOf("CALLAWAY", "PARADYM", "AI", "SMOKE"), "Callaway", "Driver", "Paradym Ai Smoke MAX Driver", "10.5°", true, "2024"),
        ClubCatalogEntry(listOf("CALLAWAY", "PARADYM"), "Callaway", "Driver", "Paradym Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("CALLAWAY", "ROGUE", "ST"), "Callaway", "Driver", "Rogue ST MAX Driver", "10.5°", true, "2022"),
        ClubCatalogEntry(listOf("CALLAWAY", "EPIC"), "Callaway", "Driver", "Epic Speed Driver", "10.5°", true, "2021"),
        ClubCatalogEntry(listOf("CALLAWAY", "JAWS"), "Callaway", "Wedge", "Jaws Raw Wedge", "54.0°", false, "2022"),

        // PING
        ClubCatalogEntry(listOf("PING", "G430", "MAX"), "PING", "Driver", "G430 MAX Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("PING", "G430"), "PING", "Driver", "G430 Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("PING", "G425"), "PING", "Driver", "G425 Driver", "9.0°", true, "2020"),
        ClubCatalogEntry(listOf("PING", "I230"), "PING", "Iron", "i230 Irons", "26.0°", false, "2022"),

        // Cobra
        ClubCatalogEntry(listOf("COBRA", "DARKSPEED"), "Cobra", "Driver", "Darkspeed MAX Driver", "10.5°", true, "2024"),
        ClubCatalogEntry(listOf("COBRA", "AEROJET"), "Cobra", "Driver", "Aerojet Driver", "10.5°", true, "2023"),
        ClubCatalogEntry(listOf("COBRA", "LTDX"), "Cobra", "Driver", "LTDx Driver", "10.5°", true, "2022"),

        // Mizuno
        ClubCatalogEntry(listOf("MIZUNO", "ST-MAX", "230"), "Mizuno", "Driver", "ST-MAX 230 Driver", "9.5°", true, "2024"),
        ClubCatalogEntry(listOf("MIZUNO", "JPX923"), "Mizuno", "Iron", "JPX923 Hot Metal Irons", "25.0°", false, "2023"),

        // Odyssey
        ClubCatalogEntry(listOf("ODYSSEY"), "Odyssey", "Putter", "Odyssey Putter", "3.0°", false, "2023")
    )

    // Fallback default master list if Room is empty
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
        // 1. Perform ML Kit Optical Text Recognition on cropped head bitmap
        val initialIdentified = runOcrOnBitmap(croppedBitmap, customBrands)

        // 2. Perform Kotlin CameraX Screw / Hosel Bolt Detection on cropped head bitmap
        val screwResult = ScrewDetector.detectScrewOnGolfHead(croppedBitmap, initialIdentified.rawOcrText)

        // 3. Perform background web search verification to match internet descriptions/images
        val webResult = GolfWebSearchVerifier.verifyAndMatchWeb(
            detectedBrand = initialIdentified.brand,
            detectedModel = initialIdentified.modelType,
            rawOcrText = initialIdentified.rawOcrText
        )

        // 4. Perform Visual Comparison & Web Spec Extraction
        val visualResult = GolfVisualWebSearchEngine.performVisualWebSearch(
            croppedBitmap = croppedBitmap,
            ocrText = initialIdentified.rawOcrText,
            initialBrand = initialIdentified.brand,
            initialModel = initialIdentified.modelType
        )

        val finalAdjustable = screwResult.hasScrew || webResult.isAdjustable == true || initialIdentified.isAdjustable

        return if (webResult.isVerified || visualResult.isMatched) {
            val finalModel = visualResult.modelType ?: webResult.modelType ?: initialIdentified.modelType
            val detectedClubType = detectClubTypeFromText(finalModel, initialIdentified.rawOcrText)

            initialIdentified.copy(
                brand = visualResult.brand ?: webResult.brand ?: initialIdentified.brand,
                clubType = detectedClubType,
                modelType = finalModel,
                loft = visualResult.loft ?: webResult.loft ?: initialIdentified.loft,
                isAdjustable = finalAdjustable,
                releaseYear = visualResult.releaseYear ?: webResult.releaseYear ?: initialIdentified.releaseYear,
                confidence = 0.98f,
                isWebVerified = true,
                webSourceDescription = if (webResult.isVerified) webResult.sourceDescription else visualResult.webSourceTitle,
                hasScrewDetected = screwResult.hasScrew || finalAdjustable,
                screwMessage = screwResult.detailMessage.ifBlank { "Baut Sleeve Hosel & Pemberat Terdeteksi" },
                visualMatchScore = visualResult.similarityScore
            )
        } else {
            initialIdentified.copy(
                isAdjustable = finalAdjustable,
                isWebVerified = false,
                webSourceDescription = webResult.sourceDescription,
                hasScrewDetected = screwResult.hasScrew,
                screwMessage = screwResult.detailMessage,
                visualMatchScore = 0.85f
            )
        }
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
        var bestCatalogMatch: ClubCatalogEntry? = null
        var maxMatchedKeywords = 0

        for (entry in catalog) {
            val matchedCount = entry.keywords.count { kw -> ocrText.contains(kw) }
            if (matchedCount > maxMatchedKeywords) {
                maxMatchedKeywords = matchedCount
                bestCatalogMatch = entry
            }
        }

        // Detection of bolt/screw/sleeve markings on head implies adjustable loft
        val hasBoltOrSleeve = ocrText.contains("BAUT") ||
                ocrText.contains("SCREW") ||
                ocrText.contains("BOLT") ||
                ocrText.contains("WEIGHT") ||
                ocrText.contains("TRACK") ||
                ocrText.contains("SLIDER") ||
                ocrText.contains("SLEEVE") ||
                ocrText.contains("OPTIFIT") ||
                ocrText.contains("SUREFIT") ||
                ocrText.contains("FCT") ||
                ocrText.contains("MYFLY") ||
                ocrText.contains("ADJUSTABLE") ||
                ocrText.contains("MAX") ||
                ocrText.contains("DRIVER")

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

        if (bestCatalogMatch != null && maxMatchedKeywords == 1) {
            val detectedLoft = extractLoft(ocrText) ?: bestCatalogMatch.defaultLoft
            return IdentifiedGolfClub(
                brand = bestCatalogMatch.brand,
                clubType = bestCatalogMatch.clubType,
                modelType = bestCatalogMatch.modelType,
                loft = detectedLoft,
                isAdjustable = if (hasBoltOrSleeve) true else bestCatalogMatch.isAdjustable,
                releaseYear = bestCatalogMatch.releaseYear,
                confidence = 0.75f,
                rawOcrText = ocrText
            )
        }

        // Fuzzy Brand Matching (handles OCR typos like "AYLORMADE" -> "TaylorMade")
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

            // 1. Exact substring match
            if (upperOcr.contains(upperBrand)) {
                return brand
            }

            // 2. Word-by-word fuzzy character similarity
            for (word in ocrWords) {
                if (word.length < 3) continue
                val score = calculateCharacterSimilarity(word, upperBrand)
                if (score > highestScore) {
                    highestScore = score
                    bestMatchBrand = brand
                }
            }

            // 3. Full text character similarity check (e.g. "AYLORMADE" vs "TAYLORMADE")
            val fullScore = calculateCharacterSimilarity(upperOcr.replace(Regex("\\s+"), ""), upperBrand.replace(Regex("\\s+"), ""))
            if (fullScore > highestScore) {
                highestScore = fullScore
                bestMatchBrand = brand
            }
        }

        // Accept fuzzy match if character similarity >= 65% (handles missing letters like AYLORMADE -> TaylorMade)
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
