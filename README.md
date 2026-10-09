# GPRIDE MUSIC — Rencana Aplikasi Android

GPRIDE adalah aplikasi pemutar musik **native Android untuk penggunaan pribadi**. Proyek ini tidak memakai konsep premium, monetisasi, atau pembagian MVP. Semua fitur direncanakan sebagai bagian dari satu aplikasi, lalu diimplementasikan dan diuji secara bertahap.

## Tujuan
- Memutar koleksi audio lokal secara andal.
- Menyediakan library, playlist, pencarian, favorit, dan kontrol playback.
- Menghadirkan efek visual audio yang dapat dikustomisasi.
- Menjaga aplikasi lokal-first, tanpa akun atau backend wajib.
- Menggunakan GitHub dan GitHub Actions untuk source control, pengujian, dan build APK.

## Teknologi yang disarankan
- Kotlin
- Jetpack Compose + Material 3
- AndroidX Media3 / ExoPlayer
- MediaSessionService untuk pemutaran latar belakang
- MediaStore untuk menemukan audio lokal
- Room untuk playlist, favorit, dan metadata lokal yang diperlukan
- DataStore untuk pengaturan
- JUnit dan Compose UI testing
- Gradle Kotlin DSL
- GitHub Actions untuk CI dan build artefak

Versi Android minimum dan versi toolchain akan ditetapkan saat proyek Gradle dibuat, setelah kompatibilitas plugin dan perangkat sasaran diputuskan.

## Dokumen proyek
- `docs/FEATURES.md` — daftar fitur lengkap.
- `docs/ARCHITECTURE.md` — rancangan komponen dan struktur kode.
- `docs/ROADMAP.md` — urutan implementasi tanpa label MVP/premium.
- `docs/TESTING.md` — checklist pengujian.
- `docs/GITHUB_ACTIONS.md` — pipeline CI dan keamanan.
- `.github/workflows/ci.yml` — workflow awal untuk lint, unit test, dan APK debug.

## Prinsip
1. Tidak memerlukan akun atau koneksi internet untuk pemutaran musik lokal.
2. Tidak mengunggah file musik pengguna.
3. Efek visual harus dapat dinonaktifkan dan memperhatikan baterai.
4. Fitur yang bergantung pada perangkat/API harus memiliki fallback yang jelas.
5. Jangan commit keystore, password, token, atau rahasia lainnya.

## Cara menggunakan rencana ini
1. Buat repository GitHub bernama `GPRIDE-Android`.
2. Salin dokumen dan workflow ke repository.
3. Buat proyek Android dengan Gradle Wrapper.
4. Sesuaikan versi JDK dengan Android Gradle Plugin yang dipilih.
5. Jalankan workflow dari tab **Actions** setelah struktur proyek valid.
