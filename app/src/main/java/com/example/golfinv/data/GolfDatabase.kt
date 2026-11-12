package com.example.golfinv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [GolfClubEntity::class, BrandEntity::class], version = 5, exportSchema = false)
abstract class GolfDatabase : RoomDatabase() {
    abstract fun golfClubDao(): GolfClubDao
    abstract fun brandDao(): BrandDao

    companion object {
        @Volatile
        private var INSTANCE: GolfDatabase? = null

        fun getDatabase(context: Context): GolfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GolfDatabase::class.java,
                    "golf_inventory_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial 100+ global golf brands
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).brandDao()
                            val initialBrands = listOf(
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
                            ).map { BrandEntity(name = it) }
                            dao.insertBrands(initialBrands)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
