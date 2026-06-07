package com.screenwakelock.detector.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.screenwakelock.detector.data.db.AppDatabase
import com.screenwakelock.detector.data.db.NotificationCacheDao
import com.screenwakelock.detector.data.db.WakeEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "screen_wakelock.db",
        )
            .addMigrations(MIGRATION_1_2)
            .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE wake_events ADD COLUMN rootParserId TEXT DEFAULT NULL",
            )
        }
    }

    @Provides
    fun provideWakeEventDao(database: AppDatabase): WakeEventDao =
        database.wakeEventDao()

    @Provides
    fun provideNotificationCacheDao(database: AppDatabase): NotificationCacheDao =
        database.notificationCacheDao()
}
