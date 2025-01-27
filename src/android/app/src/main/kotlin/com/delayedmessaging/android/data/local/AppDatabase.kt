package com.delayedmessaging.android.data.local

import androidx.room.Database // version: 2.6.1
import androidx.room.RoomDatabase // version: 2.6.1
import androidx.room.Room // version: 2.6.1
import android.content.Context // version: latest
import net.zetetic.database.sqlcipher.SQLiteDatabase // version: 4.5.4
import android.app.backup.BackupManager // version: latest
import com.jakewharton.timber.Timber // version: 5.0.1
import com.delayedmessaging.android.data.local.dao.MessageDao
import com.delayedmessaging.android.data.local.dao.UserDao
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.User
import java.io.File
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.KeyGenerator

/**
 * Room database implementation with SQLCipher encryption and backup support.
 * Provides thread-safe access to message and user data with comprehensive monitoring.
 */
@Database(
    entities = [Message::class, User::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao

    private val backupManager: BackupManager by lazy {
        BackupManager(context)
    }

    companion object {
        private const val DATABASE_NAME = "delayed_messaging.db"
        private const val BACKUP_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
        private const val KEY_LENGTH = 256
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private lateinit var context: Context

        /**
         * Gets the singleton database instance with encryption enabled.
         * Creates a new instance if one doesn't exist.
         *
         * @param context Application context
         * @return Encrypted database instance
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            Companion.context = context.applicationContext
            
            // Initialize encryption
            val key = getOrGenerateKey()
            SQLiteDatabase.loadLibs(context)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).apply {
                // Enable encryption
                openHelperFactory(net.zetetic.database.sqlcipher.SupportFactory(key))
                
                // Enable logging in debug mode
                setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                
                // Configure automatic backups
                enableBackup()
                
                // Add migration strategies here when needed
                fallbackToDestructiveMigration()
            }.build()
        }

        private fun getOrGenerateKey(): ByteArray {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(KEY_LENGTH, SecureRandom())
            val secretKey: SecretKey = keyGenerator.generateKey()
            return secretKey.encoded
        }
    }

    /**
     * Initiates a database backup operation.
     * @return true if backup was successful, false otherwise
     */
    fun backup(): Boolean {
        return try {
            // Close any open connections
            close()

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val backupFile = File(context.getExternalFilesDir(null), "${DATABASE_NAME}.backup")

            // Copy encrypted database file
            dbFile.inputStream().use { input ->
                backupFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Verify backup integrity
            if (backupFile.exists() && backupFile.length() > 0) {
                backupManager.dataChanged()
                Timber.i("Database backup completed successfully")
                true
            } else {
                Timber.e("Database backup failed - invalid backup file")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Database backup failed")
            false
        }
    }

    /**
     * Rotates the database encryption key.
     * @param newKey New encryption key
     * @return true if rotation was successful, false otherwise
     */
    fun rotateEncryptionKey(newKey: ByteArray): Boolean {
        return try {
            // Create temporary database with new key
            val tempDbName = "${DATABASE_NAME}.temp"
            val currentDb = context.getDatabasePath(DATABASE_NAME)
            val tempDb = context.getDatabasePath(tempDbName)

            // Re-encrypt database with new key
            SQLiteDatabase.openDatabase(
                currentDb.path,
                getOrGenerateKey(),
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { sourceDb ->
                SQLiteDatabase.openDatabase(
                    tempDb.path,
                    newKey,
                    null,
                    SQLiteDatabase.CREATE_IF_NECESSARY
                ).use { destDb ->
                    sourceDb.rawExecSQL("ATTACH DATABASE '${tempDb.path}' AS encrypted KEY '${String(newKey)}'")
                    sourceDb.rawExecSQL("SELECT sqlcipher_export('encrypted')")
                    sourceDb.rawExecSQL("DETACH DATABASE encrypted")
                }
            }

            // Verify and replace old database
            if (tempDb.exists() && tempDb.length() > 0) {
                close()
                currentDb.delete()
                tempDb.renameTo(currentDb)
                Timber.i("Database encryption key rotated successfully")
                true
            } else {
                Timber.e("Key rotation failed - invalid temporary database")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Key rotation failed")
            false
        }
    }

    private fun enableBackup() {
        // Schedule periodic backups
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            object : Runnable {
                override fun run() {
                    if (backup()) {
                        Timber.d("Scheduled backup completed successfully")
                    }
                    android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed(this, BACKUP_INTERVAL)
                }
            },
            BACKUP_INTERVAL
        )
    }
}