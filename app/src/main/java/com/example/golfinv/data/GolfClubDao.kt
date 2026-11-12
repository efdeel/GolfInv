package com.example.golfinv.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GolfClubDao {
    @Query("SELECT * FROM golf_clubs ORDER BY createdAt DESC")
    fun getAllClubs(): Flow<List<GolfClubEntity>>

    @Query("SELECT * FROM golf_clubs WHERE id = :id")
    suspend fun getClubById(id: Long): GolfClubEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClub(club: GolfClubEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubs(clubs: List<GolfClubEntity>): List<Long>

    @Update
    suspend fun updateClub(club: GolfClubEntity)

    @Delete
    suspend fun deleteClub(club: GolfClubEntity)

    @Query("DELETE FROM golf_clubs WHERE id = :id")
    suspend fun deleteClubById(id: Long)

    @Query("DELETE FROM golf_clubs")
    suspend fun deleteAllClubs()
}
