package com.gpride.player

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    @StringRes val label: Int,
    @StringRes val placeholder: Int,
    val icon: ImageVector,
) {
    Home("home", R.string.nav_home, R.string.nav_home, Icons.Filled.Home),
    Library("library", R.string.nav_library, R.string.placeholder_library, Icons.Filled.Explore),
    Playlist("playlist", R.string.nav_playlist, R.string.placeholder_playlist, Icons.Filled.MusicNote),
    Settings("settings", R.string.nav_settings, R.string.placeholder_settings, Icons.Filled.GridView),
}
