package com.gpride.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema selalu gelap, jadi ikon status bar dan bar navigasi dipaksa terang.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val app = application as GprideApplication
            // Baca sekali di awal agar tema tidak berkedip dari hijau ke warna pilihan saat aplikasi dibuka.
            val initial = remember { runBlocking { app.settings.theme.first() } }
            val theme by app.settings.theme.collectAsState(initial = initial)
            GprideTheme(accentIndex = theme.accent, amoled = theme.amoled) {
                GprideApp()
            }
        }
    }
}
