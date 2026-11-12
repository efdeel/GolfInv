package com.example.golfinv.utils

import android.content.Context
import android.util.Log
import java.io.File

object AutoBackupManager {

    private const val TAG = "AutoBackupManager"

    fun backupDatabase(context: Context) {
        try {
            val dbFile = context.getDatabasePath("golf_inventory_database")
            if (dbFile.exists()) {
                val backupDir = context.getExternalFilesDir("GolfInvDataBackup") ?: return
                if (!backupDir.exists()) backupDir.mkdirs()
                val backupFile = File(backupDir, "golf_inventory_backup.db")
                dbFile.copyTo(backupFile, overwrite = true)
                Log.d(TAG, "Auto backup database successfully created.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto backing up database", e)
        }
    }

    fun restoreDatabaseIfEmpty(context: Context) {
        try {
            val dbFile = context.getDatabasePath("golf_inventory_database")
            val backupDir = context.getExternalFilesDir("GolfInvDataBackup") ?: return
            val backupFile = File(backupDir, "golf_inventory_backup.db")

            if (backupFile.exists() && (!dbFile.exists() || dbFile.length() == 0L)) {
                // Ensure parent directory exists
                dbFile.parentFile?.mkdirs()
                backupFile.copyTo(dbFile, overwrite = true)
                Log.d(TAG, "Restored database successfully from external backup on reinstall.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database on reinstall", e)
        }
    }
}
