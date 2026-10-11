# Daftar Fitur GPRIDE

Semua fitur di bawah ini adalah bagian dari rencana aplikasi pribadi. Urutan implementasi ditentukan oleh ketergantungan teknis, bukan tingkatan premium atau MVP.

## 1. Pemutaran musik
- Play, pause, resume, seek, lagu berikutnya/sebelumnya.
- Shuffle, repeat satu lagu, repeat antrean.
- Antrean pemutaran: tambah, hapus, susun ulang, dan putar berikutnya.
- Tampilkan judul, artis, album, durasi, posisi pemutaran, dan album art.
- Tangani format audio yang didukung Android/Media3; uji MP3, M4A/AAC, FLAC, Ogg/Opus, WAV sesuai dukungan decoder.
- Tangani file yang rusak, metadata kosong, dan file yang dipindahkan/dihapus.

## 2. Library lokal
- Pindai audio melalui MediaStore.
- Tampilan berdasarkan semua lagu, album, artis, folder, dan lagu terbaru.
- Pencarian berdasarkan judul, artis, album, dan metadata yang tersedia.
- Penyortiran berdasarkan nama, artis, album, durasi, dan tanggal.
- Album art dan metadata.
- Pilihan untuk mengecualikan folder atau memperbarui pemindaian bila diperlukan.

## 3. Koleksi pribadi
- Favorit.
- Buat, ubah nama, hapus, dan susun playlist.
- Tambahkan satu atau banyak lagu ke playlist.
- Riwayat pemutaran dan lagu yang sering diputar (opsional dalam pengaturan).
- Persistensi playlist/favorit melalui Room.

## 4. Audio dan kontrol
- Equalizer dan preset jika efek audio tersedia dan kompatibel.
- Timer tidur.
- Crossfade dan gapless playback bila didukung konfigurasi player dan format audio.
- Normalisasi volume/ReplayGain jika kelak diimplementasikan dan metadata tersedia.
- Kontrol dari notifikasi media, lock screen, headset, dan perangkat Bluetooth.
- Penanganan audio focus, panggilan/interupsi, dan perubahan output audio.

## 5. Audio Visualizer
### Mode visual
1. **Spectrum Bars** — batang frekuensi.
2. **Circular Spectrum** — spektrum melingkar, opsional dengan album art di tengah.
3. **Waveform** — garis/gelombang animasi.
4. **Particle Flow** — partikel yang bergerak berdasarkan intensitas audio.
5. **Cermin** — batang simetris naik dan turun dari garis tengah.
6. **Riak** — cincin memancar dari pusat mengikuti bass.
7. **Titik** — matriks titik bergaya LED.
8. **Area** — gunung bergradasi dengan garis tepi bercahaya.
9. **Bintang** — poligon bintang berdenyut dengan jari-jari.
10. **Static/Off** — visualizer dapat dimatikan.

### Pengaturan visual
- Sensitivitas.
- Jumlah batang atau detail visual, jika relevan.
- Warna aksen dan gradien.
- Kecepatan/kelancaran animasi.
- Batas frame rate untuk membantu menghemat baterai.
- Mode hemat daya dan penghentian animasi saat layar tidak aktif/aplikasi di latar belakang.

### Implementasi dan batasan
- Pertimbangkan Android `Visualizer` API untuk menangkap waveform/FFT bila tersedia.
- Akses sesi audio harus dilakukan sesuai aturan API dan lifecycle; lepaskan objek visualizer saat tidak diperlukan.
- Ketersediaan dan hasil bisa berbeda antarperangkat, versi Android, decoder, dan jalur audio.
- Bila analisis audio tidak tersedia, fallback boleh menampilkan animasi berbasis status playback, tetapi UI harus membedakannya dari spektrum audio real-time.
- Hindari mengklaim visualisasi frekuensi akurat jika hanya menggunakan estimasi posisi/durasi lagu.
- Uji penggunaan CPU, GPU, baterai, serta stabilitas ketika berpindah aplikasi.

### Lirik otomatis (dibuat dari suara lagu)
- Tombol **Lirik** di kartu Lanjut Diputar (Beranda) membuka layar lirik. Panel lirik ringkas juga ada di layar Sedang Diputar.
- Lirik **dibuat** dari audio lagu: file audio ditranskripsi oleh model Whisper lewat Groq atau OpenAI memakai kunci API milik pengguna, menghasilkan teks berwaktu (disimpan sebagai LRC di perangkat).
- Baris yang sedang dinyanyikan disorot dan layar bergulir otomatis; ketuk baris untuk lompat ke bagian itu.
- Pengaturan: penyedia, kunci API, dan bahasa lagu di Pengaturan > Lirik otomatis. Ada tombol Buat ulang dan Hapus.
- Privasi: audio **diunggah** ke penyedia yang dipilih hanya saat pengguna menekan Buat lirik otomatis, setelah persetujuan. Batas layanan 25 MB per file; format yang didukung mp3, mp4, m4a, wav, webm (FLAC/Ogg mungkin ditolak).
- Hasil bisa salah dengar, terutama pada musik ramai atau bahasa daerah; hasilnya bukan lirik resmi.

### Fitur audio (Tahap 6)
- **Timer tidur**: 15/30/45/60/90 menit atau setelah lagu ini selesai; volume dipudarkan perlahan sebelum berhenti. Dijalankan di layanan pemutar sehingga tetap bekerja saat aplikasi ditutup.
- **Equalizer**: preset (Flat, Bass Boost, Vokal, Treble, Rock, Pop, Elektronik, Akustik) atau kustom per band, plus bass boost. Bergantung dukungan perangkat.
- **Pudar volume antarlagu** 0-8 detik (bukan crossfade tumpang-tindih) dan **lewati bagian hening**. Gapless ditangani ExoPlayer bila format file mendukung.

## 6. Tampilan dan aksesibilitas
- Desain gelap sebagai tema awal; opsi AMOLED dan tema terang dapat disediakan.
- Warna aksen yang dapat dipilih.
- Home, Library, Now Playing, Playlist, dan Settings.
- Mini-player pada layar library.
- Animasi yang tidak mengganggu kontrol utama.
- Target sentuh yang nyaman, dukungan ukuran font, kontras, dan label aksesibilitas.
- Tampilan responsif untuk ukuran layar yang berbeda.

## 7. Integrasi Android
- Pemutaran latar belakang melalui `MediaSessionService`.
- Media notification dan kontrol lock screen.
- Integrasi headset/Bluetooth melalui sistem media Android.
- Widget layar utama sebagai fitur lanjutan.
- Android Auto dapat dipertimbangkan kemudian jika dibutuhkan dan setelah mengikuti persyaratan kompatibilitasnya.
- Izin media yang sesuai versi Android, dengan penjelasan yang mudah dipahami.

## 8. Privasi dan penyimpanan
- Tidak memerlukan login.
- Tidak ada backend wajib.
- Tidak mengunggah file musik.
- Minta izin seperlunya dan jelaskan tujuannya.
- Simpan playlist, favorit, dan pengaturan secara lokal.
- Jangan meminta akses ke seluruh file jika akses MediaStore yang lebih terbatas sudah cukup.

## 9. Pengaturan
- Tema dan warna aksen.
- Pilihan mode visualizer, sensitivitas, dan frame rate.
- Pengaturan library dan pemindaian.
- Timer tidur.
- Pilihan perilaku setelah headset dicabut jika diimplementasikan.
- Opsi reset pengaturan dengan konfirmasi.
