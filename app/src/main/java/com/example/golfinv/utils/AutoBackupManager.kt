package com.example.golfinv.utils

import android.content.Context
import android.util.Log
import java.io.File

object AutoBackupManager {

    private const val TAG = "AutoBackupManager"

    fun backupDatabase(context: Context) {
        try {
            }
        } catch (e: Exception) {
        }
    }

    fun restoreDatabaseIfEmpty(context: Context) {
        try {

                dbFile.parentFile?.mkdirs()
            }
        } catch (e: Exception) {
        }
    }
}
