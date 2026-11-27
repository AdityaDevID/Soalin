# Soalin

Aplikasi belajar offline berbasis Kotlin (Jetpack Compose), tampilan gelap.

- Muat soal lewat link (Google Drive, GitHub, Gist, Dropbox, link langsung) atau file dari HP
- Soal pilihan ganda; **urutan soal dan urutan pilihan jawaban diacak setiap sesi**
- Soal tersimpan di HP, jadi bisa belajar tanpa internet
- Skor terbaik per paket, pembahasan, dan daftar soal yang salah di akhir sesi

Cara memuat soal: lihat **[CARA_MEMUAT_SOAL.md](CARA_MEMUAT_SOAL.md)**.

## Build

- Android Studio: buka folder ini, tekan Run.
- Command line: `./gradlew assembleDebug` (APK ada di `app/build/outputs/apk/debug/`).
- GitHub Actions: workflow ada di `.github/workflows/build.yml`.

Spesifikasi: minSdk 26, targetSdk 34, Kotlin 2.0.20, AGP 8.5.2, Gradle 8.7, JDK 17.

## Struktur

```
app/src/main/java/com/soalin/app/
  MainActivity.kt
  data/   Models, QuizParser (baca JSON), Downloader (link -> unduhan), Repository
  ui/     Theme, App (navigasi), HomeScreen, QuizScreen, ResultScreen
docs/     logo.png, contoh_soal.json
```
