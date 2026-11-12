package com.example.golfinv.utils

import com.example.golfinv.data.GolfClubEntity
import java.text.NumberFormat
import java.util.Locale

object GolfStringUtils {

    fun formatPrice(amount: Long): String {
        if (amount <= 0) return "Rp 0"
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    /**
     * Formats golf club data into copyable/shareable text:
     * Brand:
     * Club Type: Driver/Fairway Wood/Rescue/Iron/Wedge/Putter
     * Model:
     * Loft Degree: 10.5° (Adjustable) / (Fixed)
     * Shaft: (diisi oleh user)
     * Grip: (diisi oleh user)
     * Kondisi: XX%
     * Modal: Rp X.XXX.XXX
     * Harga Jual: Rp X.XXX.XXX
     * Status: Tersedia / Terjual
     */
    fun formatClubAsText(club: GolfClubEntity): String {
        val sb = StringBuilder()
        sb.append("Brand: ").append(club.brand.ifBlank { "-" }).append("\n")
        sb.append("Club Type: ").append(club.clubType.ifBlank { "Driver" }).append("\n")
        sb.append("Model: ").append(club.modelType.ifBlank { "-" }).append("\n")

        val adjustableStr = if (club.isAdjustable) " (Adjustable)" else " (Fixed)"
        sb.append("Loft Degree: ").append(club.loft.ifBlank { "-" }).append(adjustableStr).append("\n")

        sb.append("Shaft: ").append(club.shaft.ifBlank { "-" }).append("\n")
        sb.append("Grip: ").append(club.grip.ifBlank { "-" }).append("\n")
        sb.append("Kondisi: ").append(club.conditionPercent).append("%\n")
        if (club.costPrice > 0) {
            sb.append("Modal: ").append(formatPrice(club.costPrice)).append("\n")
        }
        if (club.sellingPrice > 0) {
            sb.append("Harga Jual: ").append(formatPrice(club.sellingPrice)).append("\n")
        }
        val statusStr = if (club.isSold) "Terjual (Sold Out)" else "Tersedia (Available)"
        sb.append("Status: ").append(statusStr)
        if (club.notes.isNotBlank()) {
            sb.append("\nCatatan: ").append(club.notes)
        }
        return sb.toString()
    }
}
