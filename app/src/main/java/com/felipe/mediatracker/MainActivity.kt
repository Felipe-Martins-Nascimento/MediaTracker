package com.felipe.mediatracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.felipe.mediatracker.ui.navigation.AppNavigation
import com.felipe.mediatracker.ui.theme.MediaTrackerTheme

/** Tela principal do app (Etapa 2, em Jetpack Compose). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaTrackerTheme {
                AppNavigation()
            }
        }
    }
}
