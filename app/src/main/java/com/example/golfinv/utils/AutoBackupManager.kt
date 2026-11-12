package com.example.golfinv.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object AutoBackupManager {

    private const val TAG = "AutoBackupManager"
    private const val DB_NAME = "golf_inventory_database"
    private const val BACKUP_DB_NAME = "golf_inventory_database_backup.db"

    /**
     * Backs up the Room SQLite database file to app internal storage.
     */
    fun backupDatabase(context: Context) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return

            val backupFile = File(context.filesDir, BACKUP_DB_NAME)
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Database successfully backed up")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup database", e)
        }
    }

    /**
     * Restores internal backup database if the database file does not exist or is empty.
     */
    fun restoreDatabaseIfEmpty(context: Context) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val backupFile = File(context.filesDir, BACKUP_DB_NAME)

            if ((!dbFile.exists() || dbFile.length() == 0L) && backupFile.exists()) {
                dbFile.parentFile?.mkdirs()
                FileInputStream(backupFile).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Database successfully restored from backup")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore database from backup", e)
        }
    }
}
