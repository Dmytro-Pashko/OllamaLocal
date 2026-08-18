package com.dpashko.localaiclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dpashko.localaiclient.presentation.LocalAiClientRoot
import com.dpashko.localaiclient.ui.theme.LocalAiClientTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main launcher activity hosting the Compose app shell.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            LocalAiClientTheme {
                LocalAiClientRoot()
            }
        }
    }

    /**
     * Requests notification permission needed for foreground generation status on Android 13+.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS,
        )
    }

    private companion object {
        const val REQUEST_POST_NOTIFICATIONS = 100
    }
}
