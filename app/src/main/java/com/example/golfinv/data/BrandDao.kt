package com.example.golfinv.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandDao {
    @Query("SELECT * FROM golf_brands ORDER BY name ASC")
    fun getAllBrands(): Flow<List<BrandEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBrand(brand: BrandEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBrands(brands: List<BrandEntity>)

    @Update
    suspend fun updateBrand(brand: BrandEntity)

    @Delete
    suspend fun deleteBrand(brand: BrandEntity)

    @Query("""
        DELETE FROM golf_brands WHERE id NOT IN (
            SELECT MIN(id) FROM golf_brands GROUP BY UPPER(name)
        )
    """)
    suspend fun removeDuplicateBrands()
}
