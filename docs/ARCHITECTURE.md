# Arsitektur GPRIDE

## Prinsip
Pisahkan UI, aturan aplikasi, sumber data, dan mesin playback. Tujuannya agar tampilan dapat berubah tanpa merusak playback, dan pengujian logika dapat dilakukan tanpa perangkat audio sungguhan.

## Komponen
- **Presentation**: Jetpack Compose, Navigation, ViewModel, state UI.
- **Domain**: use case dan aturan library/playlist.
- **Playback**: Media3 ExoPlayer dan MediaSessionService.
- **Local data**: MediaStore, Room, DataStore.
- **Visualizer**: pengelola capture audio/FFT dan renderer visual; lifecycle terikat ke layar serta sesi audio.
- **Testing**: unit tests untuk aturan dan repository; instrumented/UI tests untuk alur Android.

## Struktur direktori yang disarankan

```text
GPRIDE-Android/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/<package>/gpride/
│       │   │   ├── ui/
│       │   │   │   ├── home/
│       │   │   │   ├── library/
│       │   │   │   ├── player/
│       │   │   │   ├── playlist/
│       │   │   │   ├── visualizer/
│       │   │   │   └── settings/
│       │   │   ├── playback/
│       │   │   ├── data/
│       │   │   ├── domain/
│       │   │   └── di/
│       │   └── AndroidManifest.xml
│       └── test/
├── gradle/
├── .github/workflows/
├── docs/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

`<package>` adalah placeholder; ganti dengan package name yang dipilih sebelum membuat kode final.

## Playback
- Satu komponen playback menjadi sumber kebenaran untuk antrean dan status lagu.
- Gunakan Media3 untuk pemutaran serta integrasi media session.
- `MediaSessionService` menangani lifecycle playback di latar belakang.
- UI mengamati status player melalui state/ViewModel, bukan membuat player terpisah pada setiap layar.
- Kelola audio focus dan interupsi dengan perilaku yang konsisten.

## Library dan data
- MediaStore untuk menemukan file audio yang diizinkan.
- Room untuk playlist, favorit, riwayat, dan metadata lokal tambahan.
- DataStore untuk pengaturan sederhana.
- Jangan menduplikasi metadata yang sudah tersedia tanpa alasan; perbarui cache saat library berubah atau saat pemindaian diminta.

## Visualizer
- Pisahkan sumber data visualizer dari renderer.
- Gunakan API capture yang sesuai dan periksa ketersediaan efek.
- Mulai/berhenti capture sesuai lifecycle; jangan membiarkan listener atau efek berjalan setelah layar ditutup.
- Batasi update UI dan frame rate.
- Sediakan fallback non-spectrum jika capture tidak tersedia.

## Konfigurasi awal
Pilih minSdk, targetSdk, Android Gradle Plugin, Gradle, Kotlin, dan JDK sebagai satu kombinasi kompatibel. Hindari mengunci versi secara acak sebelum proyek Gradle dibuat. Gunakan package ID unik dan stabil.
