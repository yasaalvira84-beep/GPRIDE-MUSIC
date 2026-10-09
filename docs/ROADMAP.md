# Urutan Implementasi GPRIDE

Tidak ada klasifikasi MVP/premium. Ini urutan kerja teknis untuk membangun keseluruhan aplikasi dengan risiko yang lebih terkendali.

## Tahap 1 — Fondasi
- Buat repository dan proyek Android Kotlin.
- Tentukan package ID dan toolchain yang kompatibel.
- Terapkan tema dan navigasi dasar.
- Tambahkan CI GitHub Actions.
- Pastikan debug APK berhasil dibangun.

## Tahap 2 — Mesin playback
- Integrasikan Media3.
- Implementasikan play/pause, seek, next/previous, repeat, shuffle, dan antrean.
- Tambahkan MediaSessionService.
- Uji notifikasi media, lock screen, audio focus, dan pemutaran latar belakang.

## Tahap 3 — Library dan koleksi
- Pindai file audio lewat MediaStore.
- Tampilkan lagu, album, artis, dan folder.
- Tambahkan pencarian dan penyortiran.
- Implementasikan Room, favorit, playlist, dan riwayat sesuai kebutuhan.

## Tahap 4 — UI pemutar lengkap
- Home, Library, Now Playing, Playlist, dan Settings.
- Mini-player, metadata, album art, dan kontrol antrean.
- Tema, warna aksen, serta aksesibilitas.

## Tahap 5 — Audio Visualizer
- Buat renderer Spectrum Bars terlebih dahulu.
- Uji akses dan lifecycle Android Visualizer API pada perangkat sasaran.
- Tambahkan Circular Spectrum, Waveform, dan Particle Flow.
- Sediakan kontrol sensitivitas, warna, detail, frame rate, dan opsi off.
- Ukur konsumsi CPU/GPU/baterai dan uji fallback.

## Tahap 6 — Fitur audio dan integrasi
- Timer tidur.
- Equalizer/preset jika tersedia.
- Crossfade/gapless jika kompatibel.
- Widget, kontrol perangkat, serta integrasi tambahan yang memang dibutuhkan.

## Tahap 7 — Stabilitas dan build rilis
- Uji perangkat nyata dan beberapa versi Android.
- Perbaiki bug format audio, izin, lifecycle, dan pemutaran latar belakang.
- Siapkan signing key dengan aman bila diperlukan.
- Build APK release melalui tag versi.
- Simpan changelog dan catatan instalasi.

## Perkiraan
Untuk satu pengembang dengan bantuan AI, fondasi dan pemutar dasar dapat dikerjakan lebih cepat daripada seluruh fitur visual/audio lanjutan. Perkiraan waktu sangat bergantung pada pengalaman dan perangkat uji; jadikan daftar tugas sebagai panduan, bukan tenggat yang dijamin.
