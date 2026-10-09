# GitHub Actions

## Workflow CI
File `.github/workflows/ci.yml` menjalankan lint, unit test, membangun APK debug, dan mengunggah APK sebagai artefak. Workflow mengasumsikan proyek Android Gradle sudah dibuat dan Gradle Wrapper tersedia.

## Cara menggunakan
1. Buat repository `GPRIDE-Android`.
2. Tambahkan proyek Android Gradle dan Gradle Wrapper.
3. Sesuaikan JDK pada workflow dengan versi Android Gradle Plugin yang digunakan.
4. Push ke branch `main`, `develop`, atau `feature/*`, atau jalankan workflow manual.
5. Buka tab **Actions** dan unduh artefak APK dari run yang berhasil.

## Release workflow yang direncanakan
- Trigger pada tag versi seperti `v1.0.0`.
- Jalankan lint, unit test, dan build release.
- Gunakan keystore signing melalui GitHub Secrets atau environment yang terlindungi.
- Publikasikan APK/AAB ke GitHub Releases jika dibutuhkan.

## Keamanan
- Jangan commit keystore atau password.
- Gunakan `permissions` minimum.
- Untuk produksi, tinjau action yang dipakai dan pin ke commit SHA yang diverifikasi.
- Lindungi environment release dengan approval bila sesuai.
- Jangan menganggap APK debug sebagai artefak distribusi publik yang sudah ditandatangani untuk rilis.
