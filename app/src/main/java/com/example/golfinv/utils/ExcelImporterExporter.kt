package com.example.golfinv.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.golfinv.data.GolfClubEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.UUID

object ExcelImporterExporter {

    private const val TAG = "ExcelImporterExporter"

    /**
     * Exports inventory to an HTML-based Excel file (.xls format) which natively renders
     * actual embedded base64 photos directly inside Excel cells when opened in Microsoft Excel / Google Sheets.
     */
    suspend fun exportToExcelXls(
        context: Context,
        uri: Uri,
        clubs: List<GolfClubEntity>,
        includePhotos: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    val htmlBuilder = StringBuilder()
                    htmlBuilder.append("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n")
                    htmlBuilder.append("<head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">\n")
                    htmlBuilder.append("<style>table { border-collapse: collapse; width: 100%; } th, td { border: 1px solid #999; padding: 8px; text-align: left; vertical-align: middle; }</style>\n")
                    htmlBuilder.append("</head>\n<body>\n")
                    htmlBuilder.append("<h2>Golf Inventory Export</h2>\n")
                    htmlBuilder.append("<table>\n")
                    htmlBuilder.append("<tr>")
                    htmlBuilder.append("<th>Brand</th><th>Club Type</th><th>Model</th><th>Loft Degree</th><th>Adjustable</th><th>Release Year</th><th>Shaft</th><th>Grip</th><th>Kondisi (%)</th><th>Modal (Rp)</th><th>Harga Jual (Rp)</th><th>Status</th><th>Catatan</th>")
                    if (includePhotos) {
                        htmlBuilder.append("<th>Foto Stik Golf</th>")
                    }
                    htmlBuilder.append("</tr>\n")

                    for (club in clubs) {
                        val adjustableStr = if (club.isAdjustable) "Adjustable" else "Fixed"
                        val statusStr = if (club.isSold) "Terjual" else "Tersedia"

                        htmlBuilder.append("<tr>")
                        htmlBuilder.append("<td>").append(escapeHtml(club.brand)).append("</td>")
                        htmlBuilder.append("<td>").append(escapeHtml(club.clubType)).append("</td>")
                        htmlBuilder.append("<td>").append(escapeHtml(club.modelType)).append("</td>")
                        htmlBuilder.append("<td>").append(escapeHtml(club.loft)).append("</td>")
                        htmlBuilder.append("<td>").append(adjustableStr).append("</td>")
                        htmlBuilder.append("<td>").append(escapeHtml(club.releaseYear)).append("</td>")
                        htmlBuilder.append("<td>").append(escapeHtml(club.shaft)).append("</td>")
                        htmlBuilder.append("<td>").append(escapeHtml(club.grip)).append("</td>")
                        htmlBuilder.append("<td>").append(club.conditionPercent).append("%</td>")
                        htmlBuilder.append("<td>").append(club.costPrice).append("</td>")
                        htmlBuilder.append("<td>").append(club.sellingPrice).append("</td>")
                        htmlBuilder.append("<td>").append(statusStr).append("</td>")
                        htmlBuilder.append("<td>").append(escapeHtml(club.notes)).append("</td>")

                        if (includePhotos) {
                            val base64Img = encodeImageToBase64(club.imagePath)
                            if (base64Img != null) {
                                htmlBuilder.append("<td><img src=\"data:image/jpeg;base64,$base64Img\" width=\"70\" height=\"70\"/></td>")
                            } else {
                                htmlBuilder.append("<td>-</td>")
                            }
                        }

                        htmlBuilder.append("</tr>\n")
                    }

                    htmlBuilder.append("</table>\n</body>\n</html>")
                    writer.write(htmlBuilder.toString())
                    writer.flush()
                }
            }
            Log.d(TAG, "Exported ${clubs.size} items with photos=$includePhotos to Excel successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting Excel with photos", e)
            false
        }
    }

    /**
     * Imports inventory from an Excel file (.xls / HTML / CSV format).
     * Decodes embedded base64 images back into local files and restores thumbnails.
     */
    suspend fun importFromExcelXls(
        context: Context,
        uri: Uri
    ): List<GolfClubEntity> = withContext(Dispatchers.IO) {
        val importedClubs = mutableListOf<GolfClubEntity>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { it.readCodeOrText() }

                if (content.contains("<table", ignoreCase = true)) {
                    // HTML Table Parser (Excel HTML export)
                    importedClubs.addAll(parseHtmlExcel(context, content))
                } else {
                    // TSV / CSV Parser fallback
                    importedClubs.addAll(parseTsvCsvExcel(context, content))
                }
            }
            Log.d(TAG, "Imported ${importedClubs.size} items with photos successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Excel file", e)
        }
        importedClubs
    }

    private fun parseHtmlExcel(context: Context, html: String): List<GolfClubEntity> {
        val list = mutableListOf<GolfClubEntity>()
        try {
            val trRegex = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.IGNORE_CASE)
            val tdRegex = Regex("<t[dh][^>]*>(.*?)</t[dh]>", RegexOption.IGNORE_CASE)
            val imgRegex = Regex("src=\"data:image/jpeg;base64,(.*?)\"", RegexOption.IGNORE_CASE)

            val trMatches = trRegex.findAll(html).drop(1) // Skip header row

            for (tr in trMatches) {
                val rowHtml = tr.groupValues[1]
                val tds = tdRegex.findAll(rowHtml).map { it.groupValues[1] }.toList()

                if (tds.size >= 12) {
                    val brand = extractPlainText(tds.getOrElse(0) { "" })
                    val clubType = extractPlainText(tds.getOrElse(1) { "Driver" })
                    val modelType = extractPlainText(tds.getOrElse(2) { "" })
                    val loft = extractPlainText(tds.getOrElse(3) { "10.5°" })
                    val adjustableStr = extractPlainText(tds.getOrElse(4) { "Adjustable" })
                    val isAdjustable = adjustableStr.contains("Adjustable", ignoreCase = true) || adjustableStr.contains("Ya", ignoreCase = true)
                    val releaseYear = extractPlainText(tds.getOrElse(5) { "2024" })
                    val shaft = extractPlainText(tds.getOrElse(6) { "" })
                    val grip = extractPlainText(tds.getOrElse(7) { "" })
                    val condition = extractPlainText(tds.getOrElse(8) { "90" }).filter { it.isDigit() }.toIntOrNull()?.coerceIn(10, 100) ?: 90
                    val cost = extractPlainText(tds.getOrElse(9) { "0" }).filter { it.isDigit() }.toLongOrNull() ?: 0L
                    val sell = extractPlainText(tds.getOrElse(10) { "0" }).filter { it.isDigit() }.toLongOrNull() ?: 0L
                    val status = extractPlainText(tds.getOrElse(11) { "Tersedia" })
                    val isSold = status.contains("Terjual", ignoreCase = true) || status.contains("Sold", ignoreCase = true)
                    val notes = extractPlainText(tds.getOrElse(12) { "" })

                    // Check for embedded base64 photo
                    var imagePath: String? = null
                    val imgMatch = imgRegex.find(rowHtml)
                    if (imgMatch != null) {
                        val base64Data = imgMatch.groupValues[1]
                        imagePath = decodeBase64ToImageFile(context, base64Data)
                    }

                    if (brand.isNotBlank() || modelType.isNotBlank()) {
                        list.add(
                            GolfClubEntity(
                                brand = brand.ifBlank { "Unbranded" },
                                clubType = clubType.ifBlank { "Driver" },
                                modelType = modelType.ifBlank { "Golf Club" },
                                loft = loft.ifBlank { "10.5°" },
                                isAdjustable = isAdjustable,
                                releaseYear = releaseYear.ifBlank { "2024" },
                                shaft = shaft,
                                grip = grip,
                                conditionPercent = condition,
                                costPrice = cost,
                                sellingPrice = sell,
                                isSold = isSold,
                                soldDate = if (isSold) System.currentTimeMillis() else null,
                                notes = notes,
                                imagePath = imagePath
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing HTML Excel", e)
        }
        return list
    }

    private fun parseTsvCsvExcel(context: Context, content: String): List<GolfClubEntity> {
        val list = mutableListOf<GolfClubEntity>()
        val lines = content.lines()
        for ((index, line) in lines.withIndex()) {
            if (index == 0) continue // Skip header
            val clean = line.replace("\uFEFF", "").trim()
            if (clean.isBlank()) continue

            val delimiter = if (clean.contains("\t")) "\t" else ","
            val tokens = clean.split(delimiter).map { it.trim().removeSurrounding("\"") }

            if (tokens.size >= 12) {
                val brand = tokens[0]
                val clubType = tokens[1]
                val modelType = tokens[2]
                val loft = tokens[3]
                val isAdjustable = tokens[4].contains("Adjustable", ignoreCase = true) || tokens[4].contains("Ya", ignoreCase = true)
                val releaseYear = tokens[5]
                val shaft = tokens[6]
                val grip = tokens[7]
                val condition = tokens[8].filter { it.isDigit() }.toIntOrNull()?.coerceIn(10, 100) ?: 90
                val cost = tokens[9].filter { it.isDigit() }.toLongOrNull() ?: 0L
                val sell = tokens[10].filter { it.isDigit() }.toLongOrNull() ?: 0L
                val isSold = tokens[11].contains("Terjual", ignoreCase = true)
                val notes = tokens.getOrElse(12) { "" }
                val imagePath = tokens.getOrElse(13) { "" }.ifBlank { null }

                list.add(
                    GolfClubEntity(
                        brand = brand,
                        clubType = clubType,
                        modelType = modelType,
                        loft = loft,
                        isAdjustable = isAdjustable,
                        releaseYear = releaseYear,
                        shaft = shaft,
                        grip = grip,
                        conditionPercent = condition,
                        costPrice = cost,
                        sellingPrice = sell,
                        isSold = isSold,
                        notes = notes,
                        imagePath = imagePath
                    )
                )
            }
        }
        return list
    }

    private fun encodeImageToBase64(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) return null
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                val bytes = file.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeBase64ToImageFile(context: Context, base64Str: String): String? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val imagesDir = File(context.filesDir, "golf_heads").apply { mkdirs() }
            val file = File(imagesDir, "imported_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun extractPlainText(htmlCell: String): String {
        return htmlCell.replace(Regex("<[^>]*>"), "").trim()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun BufferedReader.readCodeOrText(): String {
        val sb = StringBuilder()
        var line: String? = readLine()
        while (line != null) {
            sb.append(line).append("\n")
            line = readLine()
        }
        return sb.toString()
    }
}
