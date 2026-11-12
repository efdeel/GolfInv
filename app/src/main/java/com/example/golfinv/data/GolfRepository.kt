package com.example.golfinv.data

import android.content.Context
import android.graphics.Bitmap
import com.example.golfinv.utils.AutoBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GolfRepository(
    private val golfClubDao: GolfClubDao,
    private val brandDao: BrandDao,
    private val context: Context
) {
    init {
        // Restore local persistent backup if app was uninstalled and reinstalled
        AutoBackupManager.restoreDatabaseIfEmpty(context)
    }

    val allClubs: Flow<List<GolfClubEntity>> = golfClubDao.getAllClubs()
    val allBrands: Flow<List<BrandEntity>> = brandDao.getAllBrands()

    suspend fun insertClub(club: GolfClubEntity): Long {
        val id = golfClubDao.insertClub(club)
        AutoBackupManager.backupDatabase(context)
        return id
    }

    suspend fun insertClubs(clubs: List<GolfClubEntity>) {
        golfClubDao.insertClubs(clubs)
        AutoBackupManager.backupDatabase(context)
    }

    suspend fun updateClub(club: GolfClubEntity) {
        golfClubDao.updateClub(club)
        AutoBackupManager.backupDatabase(context)
    }

    suspend fun deleteClub(club: GolfClubEntity) {
        club.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        golfClubDao.deleteClub(club)
        AutoBackupManager.backupDatabase(context)
    }

    suspend fun deleteAllClubs() {
        val imagesDir = File(context.filesDir, "golf_heads")
        if (imagesDir.exists()) {
            imagesDir.deleteRecursively()
        }
        golfClubDao.deleteAllClubs()
        AutoBackupManager.backupDatabase(context)
    }

    /**
     * Seeds default brands only once on first install.
     * Preserves all user edits, custom additions, and deletions across app updates.
     */
    suspend fun ensureDefaultBrandsSeeded() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("golf_inv_prefs", Context.MODE_PRIVATE)
        val isSeeded = prefs.getBoolean("default_brands_seeded_v5", false)

        if (!isSeeded) {
            val defaultBrands = listOf(
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
                "AG High Performance", "Shenzhen Impact Golf"
            ).map { BrandEntity(name = it) }

            brandDao.insertBrands(defaultBrands)
            prefs.edit().putBoolean("default_brands_seeded_v5", true).apply()
        }

        // Clean up any duplicates just in case
        brandDao.removeDuplicateBrands()
        AutoBackupManager.backupDatabase(context)
    }

    suspend fun insertBrand(name: String) {
        if (name.isNotBlank()) {
            brandDao.insertBrand(BrandEntity(name = name.trim()))
            AutoBackupManager.backupDatabase(context)
        }
    }

    suspend fun updateBrand(brand: BrandEntity, newName: String) {
        brandDao.removeDuplicateBrands()
        if (newName.isNotBlank()) {
            brandDao.updateBrand(brand.copy(name = newName.trim()))
            AutoBackupManager.backupDatabase(context)
        }
    }

    suspend fun deleteBrand(brand: BrandEntity) {
        brandDao.deleteBrand(brand)
        AutoBackupManager.backupDatabase(context)
    }

    suspend fun saveHeadBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "golf_heads").apply { mkdirs() }
        val fileName = "head_${UUID.randomUUID()}.jpg"
        val imageFile = File(imagesDir, fileName)

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        imageFile.absolutePath
    }
}
