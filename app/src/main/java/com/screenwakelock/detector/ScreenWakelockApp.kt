package com.screenwakelock.detector

import android.app.Application
import com.screenwakelock.detector.worker.RetentionWorker
import com.screenwakelock.detector.worker.ShieldDigestWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ScreenWakelockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetentionWorker.schedule(this)
        ShieldDigestWorker.schedule(this)
        com.screenwakelock.detector.worker.MorningDigestWorker.schedule(this)
    }
}
