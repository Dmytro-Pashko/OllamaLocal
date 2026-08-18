package com.dpashko.localaiclient

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.slf4j.LoggerFactory
import javax.inject.Inject

/**
 * Application entry point that wires Hilt and WorkManager for background generation.
 */
@HiltAndroidApp
class LocalAiClientApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        LoggerFactory.getLogger(LocalAiClientApplication::class.java)
            .info("LocalAiClientApplication started")
    }
}
