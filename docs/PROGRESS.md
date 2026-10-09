# Progres Implementasi GPRIDE

## Tahap 1 — Fondasi (kerangka selesai, belum terverifikasi build)
- [x] Proyek Android Kotlin + Gradle Kotlin DSL + version catalog
- [x] Toolchain: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, JDK 17, compileSdk/targetSdk 35, minSdk 26
- [x] Package ID: `com.gpride.player`
- [x] Tema Material 3 (gelap default) dan navigasi bawah: Beranda, Library, Playlist, Pengaturan
- [x] Struktur kode datar (satu paket); subfolder baru dibuat saat dibutuhkan
- [x] CI GitHub Actions (lint, unit test, debug APK), dengan bootstrap Gradle wrapper
- [ ] `./gradlew lintDebug`, `testDebugUnitTest`, `assembleDebug` berhasil di GitHub Actions
- [ ] Debug APK terpasang di perangkat nyata

## Tahap 2 — Mesin playback (kode selesai, belum diuji di perangkat)
- [x] Media3 1.4.1: `PlaybackService` (MediaSessionService + ExoPlayer), audio focus, jeda saat headset dicabut
- [x] Kontrol: play/pause, seek, next/previous, repeat (mati/antrean/satu lagu), shuffle
- [x] Antrean: tambah (pemilih berkas sementara), hapus, geser naik/turun, putar dari antrean
- [x] UI uji di tab Beranda; izin notifikasi diminta di Android 13+
- [ ] Uji di perangkat: notifikasi media, lock screen, audio focus/panggilan, headset, latar belakang (lihat TESTING.md)
- [ ] Item "Putar berikutnya" di antrean: ditunda sampai Tahap 3 (butuh daftar lagu)

## Tahap 3 — Library dan koleksi (kode selesai, belum diuji di perangkat)
- [x] Pemindaian MediaStore (izin `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` ≤ API 32), tanpa akses seluruh file
- [x] Tab Lagu, Album, Artis, Folder, Terbaru; pencarian (judul/artis/album) dan sortir (judul, artis, album, durasi, tanggal)
- [x] Pilih banyak lagu (tekan lama) → tambah ke antrean atau playlist; menu per lagu: putar berikutnya, antrean, playlist
- [x] Room: favorit, playlist (buat, ubah nama, hapus, susun ulang), riwayat, folder dikecualikan
- [x] DataStore: pengaktifan riwayat; Pengaturan: hapus riwayat, pindai ulang
- [x] File rusak/hilang dilewati dengan pesan; riwayat tidak menduplikasi metadata lagu
- [ ] Uji di perangkat: izin ditolak/diberikan/diubah, library besar, file dipindah/dihapus, format MP3/M4A/FLAC/Ogg/WAV
- [ ] Album art dan metadata lengkap: Tahap 4

