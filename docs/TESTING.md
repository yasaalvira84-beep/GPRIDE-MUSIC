# Checklist Pengujian GPRIDE

Tandai item saat sudah diuji pada implementasi nyata.

## Pemutaran
- [ ] Play, pause, resume, seek.
- [ ] Next/previous dan perilaku saat lagu terakhir.
- [ ] Shuffle, repeat satu lagu, repeat antrean.
- [ ] Antrean bisa ditambah, dihapus, dan disusun ulang.
- [ ] File rusak/tidak didukung tidak membuat aplikasi crash.
- [ ] Metadata atau album art yang hilang ditangani dengan baik.

## Format dan library
- [ ] MP3.
- [ ] M4A/AAC.
- [ ] FLAC.
- [ ] Ogg/Opus dan WAV sesuai dukungan perangkat.
- [ ] File dipindah atau dihapus setelah dipindai.
- [ ] Izin media ditolak, diberikan, atau diubah setelahnya.
- [ ] Library besar tidak membuat UI macet.

## Background dan integrasi
- [ ] Playback berlanjut saat layar terkunci.
- [ ] Media notification dan lock screen.
- [ ] Headset kabel/Bluetooth.
- [ ] Audio focus, panggilan, dan interupsi.
- [ ] Aplikasi dihentikan/dibuka kembali dengan state yang masuk akal.
- [ ] Perilaku setelah headset dicabut sesuai pengaturan yang dipilih.

## Koleksi
- [ ] Favorit bertahan setelah aplikasi dibuka kembali.
- [ ] Buat, ubah nama, dan hapus playlist.
- [ ] Tambah/hapus lagu dari playlist.
- [ ] Riwayat pemutaran jika diaktifkan.
- [ ] Pengaturan DataStore bertahan.

## Visualizer
- [ ] Spectrum Bars pada perangkat sasaran.
- [ ] Circular Spectrum dan album art.
- [ ] Waveform.
- [ ] Particle Flow.
- [ ] Mode visualizer dapat dimatikan.
- [ ] Fallback jika capture/Visualizer API tidak tersedia.
- [ ] Capture dan renderer berhenti saat tidak diperlukan.
- [ ] Tidak ada kebocoran resource setelah berpindah layar.
- [ ] FPS dan penggunaan baterai diuji.
- [ ] Tidak mengklaim spektrum real-time bila hanya animasi estimasi.

## Build
- [ ] `./gradlew lintDebug`
- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] Instal APK hasil build pada perangkat nyata.
- [ ] Release signing diverifikasi sebelum distribusi.
