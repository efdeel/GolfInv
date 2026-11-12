package com.example.golfinv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "golf_brands")
data class BrandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
