package com.screenwakelock.detector.di

import android.content.Context
import androidx.room.Room
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
        ).build()

    @Provides
    fun provideWakeEventDao(database: AppDatabase): WakeEventDao =
        database.wakeEventDao()

    @Provides
    fun provideNotificationCacheDao(database: AppDatabase): NotificationCacheDao =
        database.notificationCacheDao()
}
