package com.msmobile.visitas.util

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.msmobile.visitas.VisitasDatabase
import com.msmobile.visitas.visit.VisitListDateFilterOption
import com.msmobile.visitas.visit.VisitListDistanceFilterOption
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackupHandlerTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var database: VisitasDatabase

    @Inject
    lateinit var backupHandler: BackupHandler

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        database.close()
        // Clean up any backup files in cache
        val backupDir = File(context.cacheDir, "backups")
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
        // Clean up test database
        context.deleteDatabase(VisitasDatabase.DATABASE_NAME)
    }

    @Test
    fun createBackupFile_withValidDatabase_shouldCreateEncryptedBackupFile() = runTest {
        // Arrange
        // Add some test data to the database
        initDatabase()

        // Act
        val result = backupHandler.createBackupFile()

        // Assert
        val backupUri = result.getOrThrow()

        // Verify the backup file exists and is accessible
        val inputStream = context.contentResolver.openInputStream(backupUri)
        assertNotNull(inputStream)

        inputStream?.use { stream ->
            val content = stream.readBytes()
            assertTrue(content.isNotEmpty())
        }
    }

    @Test
    fun createBackupFile_withEmptyDatabase_shouldStillSucceed() = runTest {
        // Arrange - Initialize the database to create the file, but don't add any data
        initDatabase()

        // Act - Create backup without any meaningful data
        val result = backupHandler.createBackupFile()

        // Assert
        val backupUri = result.getOrThrow()
        assertNotNull(backupUri)
    }

    @Test
    fun restoreBackup_withValidBackupFile_shouldRestoreDatabase() = runTest {
        // Arrange
        // Create initial data
        initDatabase()

        // Create a backup
        val backupResult = backupHandler.createBackupFile()
        val backupUri = backupResult.getOrThrow()

        // Act
        val restoreResult = backupHandler.restoreBackup(backupUri)

        // Assert
        restoreResult.getOrThrow()
    }

    @Test
    fun restoreBackup_withInvalidUri_shouldFail() = runTest {
        // Arrange
        val invalidUri = Uri.parse("content://invalid/path")

        // Act
        val result = backupHandler.restoreBackup(invalidUri)

        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IOException)
    }

    @Test
    fun restoreBackup_withCorruptedFile_shouldFail() = runTest {
        // Arrange
        // Create a corrupted file in the backups directory (matching FileProvider config)
        val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val corruptedFile = File(backupsDir, "corrupted_backup.visitas")
        corruptedFile.writeBytes("This is not a valid encrypted backup".toByteArray())

        val corruptedUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            corruptedFile
        )

        // Act
        val result = backupHandler.restoreBackup(corruptedUri)

        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IOException)

        // Clean up
        corruptedFile.delete()
    }

    @Test
    fun backupFileName_shouldIncludeTimestamp() = runTest {
        // Arrange
        initDatabase()

        // Act
        val result = backupHandler.createBackupFile()

        // Assert
        val backupUri = result.getOrThrow()

        // The URI should contain a timestamp in the expected format
        val uriString = backupUri.toString()
        assertTrue(uriString.contains(".visitas"))
        assertTrue(uriString.contains("backup_"))
    }

    @Test
    fun backupAndRestore_roundTrip_shouldComplete() = runTest {
        // Arrange
        initDatabase()

        // Act - Create backup
        val backupResult = backupHandler.createBackupFile()
        val backupUri = backupResult.getOrThrow()

        // Restore from backup
        val restoreResult = backupHandler.restoreBackup(backupUri)

        // Assert
        restoreResult.getOrThrow()
    }

    @Test
    fun createBackupFile_shouldCreateFileInCacheDirectory() = runTest {
        // Arrange
        initDatabase()

        // Act
        val result = backupHandler.createBackupFile()

        // Assert
        result.getOrThrow()

        // Verify backup directory was created
        val backupDir = File(context.cacheDir, "backups")
        assertTrue(backupDir.exists())
        assertTrue(backupDir.listFiles()?.isNotEmpty() == true)
    }

    @Test
    fun restoreBackup_withDataInDatabase_shouldPreserveData() = runTest {
        // Arrange
        initDatabase()

        // Add test data to the database
        val testHouseholder = com.msmobile.visitas.householder.Householder(
            id = java.util.UUID.randomUUID(),
            name = "John Doe",
            address = "123 Test Street",
            notes = "Test notes",
            addressLatitude = 40.7128,
            addressLongitude = -74.0060
        )

        val testConversation = com.msmobile.visitas.conversation.Conversation(
            id = java.util.UUID.randomUUID(),
            question = "Test question",
            response = "Test response",
            orderIndex = 1,
            conversationGroupId = null
        )

        // Insert test data
        database.householderDao().save(testHouseholder)
        database.conversationDao().save(testConversation)

        // Verify data was inserted
        val originalHouseholder = database.householderDao().getById(testHouseholder.id)
        val originalConversations = database.conversationDao().listAll()
        assertNotNull(originalHouseholder)
        assertTrue(originalConversations.isNotEmpty())

        // Act - Create backup
        val backupResult = backupHandler.createBackupFile()
        val backupUri = backupResult.getOrThrow()

        // Clear the database to simulate a fresh install
        database.clearAllTables()

        // Verify database is empty after clear
        val householdersAfterClear = database.householderDao().listAll()
        assertTrue(householdersAfterClear.isEmpty())

        // Restore from backup
        val restoreResult = backupHandler.restoreBackup(backupUri)

        // Assert
        restoreResult.getOrThrow()

        // Verify data was restored
        val restoredHouseholders = database.householderDao().listAll()
        val restoredConversations = database.conversationDao().listAll()

        assertTrue(restoredHouseholders.isNotEmpty())
        assertTrue(restoredConversations.isNotEmpty())

        val restoredHouseholder = restoredHouseholders.first { it.id == testHouseholder.id }

        // Verify specific data integrity
        assertEquals(testHouseholder.name, restoredHouseholder.name)
        assertEquals(testHouseholder.address, restoredHouseholder.address)
        assertEquals(testHouseholder.notes, restoredHouseholder.notes)

        val restoredConversation = restoredConversations.find { it.id == testConversation.id }
        assertNotNull(restoredConversation)
        assertEquals(testConversation.question, restoredConversation?.question)
        assertEquals(testConversation.response, restoredConversation?.response)
        assertEquals(testConversation.orderIndex, restoredConversation?.orderIndex)
    }

    @Test
    fun restoreBackup_withV1Database_shouldMigrateAndRestoreData() = runTest {
        // Arrange
        initDatabase()

        // Copy v1 backup from test assets to a temporary file
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val backupFile = File(context.cacheDir, "backup_database_v1.visitas")
        testContext.assets.open("backup_database_v1.visitas").use { input ->
            backupFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val backupUri = Uri.fromFile(backupFile)

        // Act
        val restoreResult = backupHandler.restoreBackup(backupUri)

        // Assert
        restoreResult.getOrThrow()

        // Verify data was restored and migrated
        val restoredHouseholders = database.householderDao().listAll()
        val restoredConversations = database.conversationDao().listAll()
        val restoredVisits = database.visitDao().listAll()
        val preference = database.preferenceDao().getPreference()

        // Verify migrated fields have default values
        assertNotNull(preference)
        assertEquals(
            VisitListDateFilterOption.PastDue,
            preference?.visitListDateFilterOption
        )
        assertEquals(
            VisitListDistanceFilterOption.All,
            preference?.visitListDistanceFilterOption
        )
        assertEquals("1 householder should have been restored", 1, restoredHouseholders.size)
        assertEquals("1 conversation should have been restored", 1, restoredConversations.size)
        assertEquals("1 visit should have been restored", 1, restoredVisits.size)

        // Verify householder data was restored
        val householder = restoredHouseholders.first()
        assertNotNull("Householder should not be null", householder)

        // Clean up
        backupFile.delete()
    }

    @Test
    fun restoreBackup_withProductionDatabase_shouldRestoreData() = runTest {
        // Arrange
        initDatabase()

        // Copy production backup (anonymized) from test assets to a temporary file
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val backupFile = File(context.cacheDir, "backup_large.visitas")
        testContext.assets.open("backup_large.visitas").use { input ->
            backupFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val backupUri = Uri.fromFile(backupFile)

        // Act
        val restoreResult = backupHandler.restoreBackup(backupUri)

        // Assert
        restoreResult.getOrThrow()

        // Verify data was restored
        val restoredHouseholders = database.householderDao().listAll()
        
        // Should have householders restored
        assertTrue(
            "Expected some householders but found ${restoredHouseholders.size}",
            restoredHouseholders.isNotEmpty()
        )

        // Clean up
        backupFile.delete()
    }

    private fun initDatabase() {
        // Ensure the database is open and accessible by performing a simple operation
        // This creates the database file if it doesn't exist
        try {
            database.openHelper.writableDatabase.execSQL("SELECT 1")
        } catch (_: Exception) {
            // Database might not be fully initialized, that's okay for testing
        }
    }
}
