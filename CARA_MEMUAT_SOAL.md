# Cara Memuat Soal di Soalin

Soalin membaca soal dari **file JSON**. Soal bisa dimuat dari **link** (Google Drive, GitHub, Dropbox, atau link langsung) atau dari **file di HP**. Setelah dimuat, soal tersimpan di HP dan bisa dipakai **tanpa internet**.

## Langkah cepat

1. Buka Soalin, tekan tombol **Muat Soal**.
2. Tempel link file soal, lalu tekan **Muat**.
   - Atau tekan **Atau pilih file dari HP** untuk memilih file `.json` dari penyimpanan.
3. Paket soal muncul di beranda. Tekan paket itu untuk mulai belajar.

Memuat ulang file dengan **judul yang sama** akan menimpa paket lama, jadi cara meng-update soal cukup muat lagi link yang sama.

---

## Format file soal (JSON)

```json
{
  "judul": "PKN Kelas 2 SD - Semester 1",
  "deskripsi": "50 soal pilihan ganda",
  "soal": [
    {
      "pertanyaan": "Dasar negara Indonesia adalah ...",
      "pilihan": ["Merah Putih", "Garuda", "Pancasila"],
      "jawaban": "C",
      "pembahasan": "Dasar negara Indonesia adalah Pancasila."
    }
  ]
}
```

| Field | Wajib? | Keterangan |
|---|---|---|
| `judul` | Disarankan | Nama paket. Judul yang sama = menimpa paket lama. |
| `deskripsi` | Tidak | Keterangan singkat di kartu paket. |
| `soal` | Ya | Daftar soal. |
| `pertanyaan` | Ya | Teks soal. |
| `pilihan` | Ya | Daftar pilihan jawaban, minimal 2 (bebas 3, 4, 5, dst). |
| `jawaban` | Ya | Jawaban benar: huruf `"A"`, `"B"`, `"C"`, ... sesuai urutan di file. Angka juga boleh (`1` = A, `2` = B). Teks pilihan yang persis sama juga diterima. |
| `pembahasan` | Tidak | Penjelasan yang muncul setelah menjawab. |

Catatan penting:

- `jawaban` mengacu ke urutan pilihan **di file**. Di aplikasi, urutan soal dan pilihan **diacak setiap sesi**, dan aplikasi otomatis melacak jawaban benarnya. Jadi tidak perlu menulis soal dalam urutan acak.
- Tulis pilihan **tanpa** awalan "A." / "B.". Aplikasi menambahkan hurufnya sendiri.
- Gunakan tanda kutip lurus `"`. Kutip melengkung (`“ ”`) membuat JSON error.
- Simpan file dengan encoding UTF-8.

---

## Memuat dari Google Drive

1. Upload file `.json` ke Google Drive.
2. Tekan lama / titik tiga pada file, pilih **Bagikan (Share)**.
3. Ubah akses umum menjadi **Siapa saja yang memiliki link (Anyone with the link)** dengan peran **Viewer**.
4. Tekan **Salin link**.
5. Tempel di Soalin, tekan **Muat**.

Link Drive biasa seperti ini sudah didukung dan diubah otomatis ke link unduhan:

```
https://drive.google.com/file/d/ID_FILE/view?usp=sharing
https://drive.google.com/open?id=ID_FILE
```

Jika muncul "Akses ditolak" atau "halaman web, bukan file soal", berarti file belum dibagikan ke publik.

Tips: jika kamu mengganti isi file di Drive, pakai menu **Kelola versi** (Manage versions) agar link tetap sama. Jangan hapus lalu upload ulang, karena link-nya berubah.

## Memuat dari GitHub

Cukup tempel link file di GitHub, boleh yang halaman biasa:

```
https://github.com/USERNAME/REPO/blob/main/soal/pkn.json
```

Soalin mengubahnya otomatis ke link raw. Link raw langsung juga boleh:

```
https://raw.githubusercontent.com/USERNAME/REPO/main/soal/pkn.json
```

Link **Gist** (`https://gist.github.com/USERNAME/ID`) juga didukung. Repository harus **public**.

## Memuat dari Dropbox

Bagikan file lalu salin link-nya. Soalin mengubah `dl=0` menjadi `dl=1` otomatis.

## Link langsung lainnya

Server apa pun yang mengembalikan isi file JSON langsung (hosting sendiri, Netlify, Vercel, Supabase Storage, dan sebagainya) bisa dipakai. Syaratnya link harus membuka file JSON-nya langsung, bukan halaman web.

## Memuat dari file di HP

Tekan **Muat Soal**, lalu **Atau pilih file dari HP**, pilih file `.json`. Cara ini tidak butuh internet sama sekali.

---

## Masalah umum

| Pesan | Penyebab dan solusi |
|---|---|
| Link mengarah ke halaman web, bukan file soal | File Drive belum dibagikan ke publik, atau link bukan link file. Ulangi langkah berbagi di atas. |
| Akses ditolak (HTTP 403) | File tidak publik. Ubah menjadi "Anyone with the link". |
| File tidak ditemukan (HTTP 404) | Link salah, atau file/repo dihapus atau private. |
| Format JSON tidak valid | Ada tanda kutip melengkung, koma berlebih, atau kurung yang tidak lengkap. Cek file di jsonlint.com. |
| Soal nomor N: "jawaban" tidak cocok | Huruf jawaban melebihi jumlah pilihan, misalnya jawaban "D" padahal pilihan hanya 3. |
| Field "soal" tidak ditemukan | Daftar soal harus berada di dalam kunci `"soal"`. |

Batas ukuran file: 5 MB.

---

## Membuat file soal dengan bantuan AI

Kamu bisa menyuruh AI mengubah soal dari PDF/Word menjadi format Soalin. Contoh perintah:

> Ubah soal pilihan ganda berikut menjadi JSON dengan format: {"judul": "...", "deskripsi": "...", "soal": [{"pertanyaan": "...", "pilihan": ["...", "...", "..."], "jawaban": "C", "pembahasan": "..."}]}. Pilihan ditulis tanpa awalan huruf. Gunakan tanda kutip lurus. Tentukan kunci jawaban yang benar.

Periksa kunci jawabannya sebelum dipakai.

File contoh yang sudah jadi ada di `docs/contoh_soal.json`.
