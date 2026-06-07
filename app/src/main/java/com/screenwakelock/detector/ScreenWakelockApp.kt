package com.screenwakelock.detector

import android.app.Application
import com.screenwakelock.detector.worker.RetentionWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ScreenWakelockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetentionWorker.schedule(this)
    }
}
