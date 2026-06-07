package com.screenwakelock.detector.di

import com.screenwakelock.detector.root.RootAttributor
import com.screenwakelock.detector.root.RootAvailability
import com.screenwakelock.detector.root.RootCommandRunner
import com.screenwakelock.detector.root.RootShellService
import com.screenwakelock.detector.service.WakeMonitorCallbackHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWakeMonitorCallbackHolder(): WakeMonitorCallbackHolder =
        WakeMonitorCallbackHolder()

    @Provides
    @Singleton
    fun provideRootShellService(): RootShellService = RootShellService()

    @Provides
    @Singleton
    fun provideRootCommandRunner(
        rootShellService: RootShellService,
    ): RootCommandRunner = RootCommandRunner(rootShellService)

    @Provides
    @Singleton
    fun provideRootAvailability(
        rootShellService: RootShellService,
    ): RootAvailability = RootAvailability(rootShellService)

    @Provides
    @Singleton
    fun provideRootAttributor(
        rootCommandRunner: RootCommandRunner,
        rootAvailability: RootAvailability,
    ): RootAttributor = RootAttributor(rootCommandRunner, rootAvailability)
}
