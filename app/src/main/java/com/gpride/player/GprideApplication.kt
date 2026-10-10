package com.gpride.player

import android.app.Application
import androidx.room.Room

/** Pemegang singleton ringan (tanpa framework DI) untuk database, pengaturan, dan library. */
class GprideApplication : Application() {
    val database: GprideDatabase by lazy {
        Room.databaseBuilder(this, GprideDatabase::class.java, "gpride.db").build()
    }
    val dao: GprideDao get() = database.dao()
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val library: LibraryRepository by lazy { LibraryRepository(this) }
    val lyrics: LyricsRepository by lazy { LyricsRepository(this) }
}
