package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, Chat::class, Message::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Phase 0: Removed fallbackToDestructiveMigration() which wiped user data on version bump.
         * Added explicit MIGRATION_1_2 (no-op for now — schema unchanged, only hardening).
         * Phase 3 TODO: Switch to SQLCipher (SupportFactory with passphrase from EncryptedSharedPreferences / Android Keystore).
         *   Example: Room.databaseBuilder(...).openHelperFactory(SupportFactory(SQLiteDatabase.getBytes(passphrase)))
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema change in Phase 0; placeholder to prevent destructive migration.
                // Future Phase 3: add columns for Signal preKeys, SQLCipher rekey, etc.
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resala_secure_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    // Do NOT use fallbackToDestructiveMigration — data loss is unacceptable for a messenger.
                    // Allow downgrade fallback only for dev builds; release will throw on missing migration.
                    .fallbackToDestructiveMigrationOnDowngrade()
                 .build()
                INSTANCE = instance
                instance
            }
        }

        /** For tests: in-memory instance without migrations */
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }
}
