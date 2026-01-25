package com.msmobile.visitas.util

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.core.content.FileProvider
import com.msmobile.visitas.VisitasDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class BackupHandler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logger: Logger,
    private val encryptionHandler: EncryptionHandler,
    private val database: VisitasDatabase,
    private val dispatcherProvider: DispatcherProvider,
    private val localeProvider: LocaleProvider,
    private val dateTimeProvider: DateTimeProvider
) {
    suspend fun createBackupFile(): Result<Uri> {
        return withContext(dispatcherProvider.io) {
            try {
                val databaseFile = context.getDatabasePath(database.openHelper.databaseName)

                if (!databaseFile.exists()) {
                    return@withContext Result.failure(IOException("Database file not found"))
                }

                // Create a temporary file in the cache directory
                val tempDir = File(context.cacheDir, "backups").apply { mkdirs() }
                val today = dateTimeProvider.nowDate()
                val locale = localeProvider.getLocale()
                val formatter = SimpleDateFormat(DATE_TIME_FORMAT, locale)
                val formattedDate = formatter.format(today)
                val databaseDumpFileName = VisitasDatabase.DATABASE_NAME.plus(".dump")
                val databaseDumpFile = File(tempDir, databaseDumpFileName)
                val backupFileName = "backup_$formattedDate$BACKUP_FILE_EXTENSION"
                val backupFile = File(tempDir, backupFileName)

                if (!databaseDumpFile.exists()) {
                    databaseDumpFile.createNewFile()
                }

                try {
                    database.openHelper.writableDatabase.execSQL(
                        "VACUUM INTO ?", arrayOf(databaseDumpFile.absolutePath)
                    )
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    return@withContext Result.failure(
                        IOException(
                            "Failed to create database dump: ${error.message}",
                            error
                        )
                    )
                }

                if (!databaseDumpFile.exists() || databaseDumpFile.length() == 0L) {
                    return@withContext Result.failure(IOException("Database dump file is invalid."))
                }

                FileOutputStream(backupFile).use { outputStream ->
                    FileInputStream(databaseDumpFile).use { input ->
                        encryptionHandler.writeEncrypted(input, outputStream)
                    }
                }
                databaseDumpFile.delete()

                val contentUri = FileProvider.getUriForFile(
                    /* context = */ context,
                    /* authority = */ "${context.packageName}.fileprovider",
                    /* file = */ backupFile
                )
                Result.success(contentUri)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                logger.error(TAG, "Failed to create backup", error)
                Result.failure(IOException("Failed to create backup: ${error.message}", error))
            }
        }
    }

    suspend fun restoreBackup(uri: Uri): Result<Unit> {
        return withContext(dispatcherProvider.io) {
            try {
                // Create a temporary file for the decrypted backup
                val unencryptedDatabaseFile = File(context.cacheDir, "backup_temp.db")

                try {
                    val backupFileInputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("Failed to open backup file input stream")
                    backupFileInputStream.use { inputStream ->
                        FileOutputStream(unencryptedDatabaseFile).use { output ->
                            encryptionHandler.readEncrypted(inputStream, output)
                        }
                    }

                    // Open the backup as a separate database instance
                    val backupDb = VisitasDatabase.buildFromFile(context, unencryptedDatabaseFile)

                    try {
                        // Force database initialization and migrations to complete
                        backupDb.openHelper.writableDatabase

                        // Restore data atomically
                        database.restoreDataFrom(backupDb)

                        Result.success(Unit)
                    } finally {
                        // Close the backup database
                        backupDb.close()
                    }
                } finally {
                    // Clean up temporary file
                    unencryptedDatabaseFile.delete()
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                logger.error(TAG, "Failed to restore backup", error)
                Result.failure(IOException("Failed to restore backup: ${error.message}", error))
            }
        }
    }

    private suspend fun VisitasDatabase.restoreDataFrom(backupDatabase: VisitasDatabase) {
        // Clear all existing data
        this.clearAllTables()

        // Copy all householders first (visits depend on them via FK)
        val householders = backupDatabase.householderDao().listAll()
        householders.forEach { householder ->
            this.householderDao().save(householder)
        }

        // Copy all conversations
        val conversations = backupDatabase.conversationDao().listAll()
        conversations.forEach { conversation ->
            this.conversationDao().save(conversation)
        }

        // Copy all field services
        val fieldServices = backupDatabase.fieldServiceDao().listAll()
        fieldServices.forEach { fieldService ->
            this.fieldServiceDao().save(fieldService)
        }

        // Copy all visits
        val visits = backupDatabase.visitDao().listAll()
        visits.forEach { visit ->
            this.visitDao().save(visit)
        }

        // Copy all preferences
        val preferences = backupDatabase.preferenceDao().listAll()
        preferences.forEach { preference ->
            this.preferenceDao().save(preference)
        }
    }

    companion object {
        private const val TAG = "BackupHandler"
        private const val DATE_TIME_FORMAT = "yyyy-MM-dd_hh-mm"

        const val BACKUP_FILE_EXTENSION = ".visitas"
    }
}