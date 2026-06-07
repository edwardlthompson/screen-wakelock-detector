package com.screenwakelock.detector.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WakeEventEntity::class, NotificationCacheEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wakeEventDao(): WakeEventDao
    abstract fun notificationCacheDao(): NotificationCacheDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wake_events ADD COLUMN rootParserId TEXT DEFAULT NULL",
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
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
