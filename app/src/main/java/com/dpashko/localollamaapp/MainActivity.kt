package com.dpashko.localollamaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dpashko.localollamaapp.presentation.LocalOllamaAppRoot
import com.dpashko.localollamaapp.ui.theme.LocalOllamaAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalOllamaAppTheme {
                LocalOllamaAppRoot()
            }
        }
    }
}
