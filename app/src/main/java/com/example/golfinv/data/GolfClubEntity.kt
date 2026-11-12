package com.example.golfinv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "golf_clubs")
data class GolfClubEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val brand: String,                 // Merek (e.g. TaylorMade, Titleist)
    val clubType: String = "Driver",   // Club Type: Driver/Fairway Wood/Rescue/Iron/Wedge/Putter
    val modelType: String,             // Model (e.g. Qi10 Max)
    val loft: String,                  // Loft Degree (e.g. 10.5°)
    val isAdjustable: Boolean,        // Loft adjustable (Terdapat sleeve)
    val releaseYear: String,           // Tahun release (e.g. 2024)
    val shaft: String = "",            // Shaft (diisi oleh user)
    val grip: String = "",             // Grip (diisi oleh user)
    val conditionPercent: Int = 90,    // Kondisi (%) e.g. 90%
    val costPrice: Long = 0,           // Modal (Harga Beli)
    val sellingPrice: Long = 0,        // Harga Jual
    val isSold: Boolean = false,       // Switch Terjual
    val soldDate: Long? = null,        // Tanggal Terjual
    val notes: String = "",            // Catatan / Kondisi
    val imagePath: String? = null,     // Path foto cropped head stik golf
    val createdAt: Long = System.currentTimeMillis()
)
