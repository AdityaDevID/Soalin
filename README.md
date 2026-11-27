# Soalin

Aplikasi belajar offline berbasis Kotlin (Jetpack Compose), tampilan gelap.

- Muat soal lewat link (Google Drive, GitHub, Gist, Dropbox, link langsung) atau file dari HP
- Soal pilihan ganda; **urutan soal dan urutan pilihan jawaban diacak setiap sesi**
- Soal tersimpan di HP, jadi bisa belajar tanpa internet
- **Mode terkunci untuk anak:** saat sesi soal dimulai, aplikasi meminta penyematan layar (screen pinning). Setelah selesai atau keluar, penyematan dilepas dan **bel alarm berbunyi 5 detik** (lewat volume alarm) agar orang tua tahu. Bel juga berbunyi kalau penyematan dilepas paksa di tengah sesi atau aplikasi ditinggalkan.
- **Efek suara jawaban:** saat tombol Jawab ditekan, terdengar suara "ting-ting" kalau benar dan "tuut-tut" kalau salah (volume media). File suaranya ada di `app/src/main/res/raw/` (`sfx_correct.mp3`, `sfx_wrong.mp3`), tinggal ganti kalau mau suara lain
- Skor terbaik per paket, pembahasan, dan daftar soal yang salah di akhir sesi

Cara memuat soal: lihat **[CARA_MEMUAT_SOAL.md](CARA_MEMUAT_SOAL.md)**.

## Build

- Android Studio: buka folder ini, tekan Run.
- Command line: `./gradlew assembleDebug` (APK ada di `app/build/outputs/apk/debug/`).
- GitHub Actions (otomatis): setiap push ke `main`/`master`, APK dibuild lalu dikirim ke halaman **Releases** (`Soalin-v1.1.N.apk`), tanpa Actions artifact. Bisa juga dijalankan manual lewat tab Actions > Run workflow.
- APK ditandatangani dengan `soalin-release.jks`, jadi versi baru bisa langsung menimpa versi lama tanpa uninstall.

Lisensi: [MIT](LICENSE) © AdityaDevID

Spesifikasi: minSdk 26, targetSdk 34, Kotlin 2.0.20, AGP 8.5.2, Gradle 8.7, JDK 17.

## Struktur

```
app/src/main/java/com/soalin/app/
  MainActivity.kt, Guard.kt (penyematan + bel), Sfx.kt (pemutar suara benar/salah)
  data/   Models, QuizParser (baca JSON), Downloader (link -> unduhan), Repository
  ui/     Theme, App (navigasi), HomeScreen, QuizScreen, ResultScreen
docs/     logo.png, contoh_soal.json
```

## Penyematan layar (untuk orang tua)

Agar penyematan berfungsi, aktifkan dulu di HP: **Pengaturan > Keamanan (atau Keamanan & privasi) > Penyematan aplikasi / Sematkan layar (App pinning)**. Aktifkan juga **Minta PIN sebelum melepas sematan** supaya anak tidak bisa melepasnya sendiri. Saat sesi dimulai, tekan **Mulai/Sematkan** pada dialog sistem yang muncul.

## Alarm terus-menerus (keluar paksa)

- Selesai atau keluar lewat tombol aplikasi: penyematan dilepas, bel berbunyi **5 detik**.
- Penyematan dilepas paksa di tengah sesi, atau aplikasi ditinggalkan setelah sempat tersemat: bel berbunyi **terus-menerus** dan volume alarm dijaga maksimal. Tidak ada tombol untuk menghentikannya; alarm berhenti saat HP dihidupkan ulang.
- Alarm dijalankan oleh service latar depan (`GuardService`), jadi tetap bunyi walau aplikasi ditutup/ditinggalkan. Muncul notifikasi "Sesi belajar sedang berlangsung" selama sesi.
- Batasan: mematikan HP tidak bisa dideteksi/dihalangi aplikasi, dan "Paksa berhenti" atau uninstall dari pengaturan Android juga menghentikan alarm.
