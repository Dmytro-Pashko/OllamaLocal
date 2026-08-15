package com.dpashko.localollamaapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.slf4j.LoggerFactory
import javax.inject.Inject

@HiltAndroidApp
class LocalOllamaApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        LoggerFactory.getLogger(LocalOllamaApplication::class.java)
            .info("LocalOllamaApplication started")
    }
}
