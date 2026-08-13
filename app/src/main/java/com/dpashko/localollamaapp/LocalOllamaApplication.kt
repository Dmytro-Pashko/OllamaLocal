package com.dpashko.localollamaapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.slf4j.LoggerFactory

@HiltAndroidApp
class LocalOllamaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LoggerFactory.getLogger(LocalOllamaApplication::class.java)
            .info("LocalOllamaApplication started")
    }
}
