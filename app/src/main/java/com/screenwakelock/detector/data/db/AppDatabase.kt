package com.screenwakelock.detector.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WakeEventEntity::class, NotificationCacheEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wakeEventDao(): WakeEventDao
    abstract fun notificationCacheDao(): NotificationCacheDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wake_events ADD COLUMN rootParserId TEXT DEFAULT NULL",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notification_cache ADD COLUMN hasFullScreenIntent INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE notification_cache ADD COLUMN hasTurnScreenOn INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wake_events ADD COLUMN shieldOutcome TEXT DEFAULT NULL",
                )
                db.execSQL(
                    "ALTER TABLE wake_events ADD COLUMN shieldDetail TEXT DEFAULT NULL",
                )
                db.execSQL(
                    "ALTER TABLE wake_events ADD COLUMN evidencePackagesJson TEXT DEFAULT NULL",
                )
            }
        }

        fun getInstance(context: android.content.Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screen_wakelock.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}
