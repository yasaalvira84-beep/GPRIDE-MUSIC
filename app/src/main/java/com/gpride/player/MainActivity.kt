package com.gpride.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema selalu gelap, jadi ikon status bar dan bar navigasi dipaksa terang.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            GprideTheme {
                var splash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1300)
                    splash = false
                }
                Box(Modifier.fillMaxSize()) {
                    GprideApp()
                    AnimatedVisibility(visible = splash, exit = fadeOut(tween(350))) {
                        SplashScreen()
                    }
                }
            }
        }
    }
}
