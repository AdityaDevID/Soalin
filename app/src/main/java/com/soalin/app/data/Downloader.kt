package com.soalin.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Downloader {

    private const val MAX_BYTES = 5 * 1024 * 1024

    /** Mengubah link "halaman" (Drive, GitHub, Gist, Dropbox) menjadi link unduhan langsung. */
    fun resolve(input: String): String {
        var url = input.trim()
        if (url.isEmpty()) return url
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"

        // Google Drive: /file/d/ID/view
        Regex("""drive\.google\.com/file/d/([\w-]+)""").find(url)?.let {
            return driveDirect(it.groupValues[1])
        }
        // Google Drive: open?id=ID, uc?id=ID
        if (url.contains("drive.google.com") || url.contains("docs.google.com/uc")) {
            Regex("""[?&]id=([\w-]+)""").find(url)?.let { return driveDirect(it.groupValues[1]) }
        }
        // GitHub: /blob/ -> raw
        Regex("""^https?://(?:www\.)?github\.com/([^/]+)/([^/]+)/blob/(.+)$""").find(url)?.let {
            val path = it.groupValues[3].substringBefore('?').substringBefore('#')
            return "https://raw.githubusercontent.com/${it.groupValues[1]}/${it.groupValues[2]}/$path"
        }
        // GitHub Gist
        Regex("""^https?://gist\.github\.com/([^/]+)/([0-9a-fA-F]+)/?(?:[?#].*)?$""").find(url)?.let {
            return "https://gist.githubusercontent.com/${it.groupValues[1]}/${it.groupValues[2]}/raw"
        }
        // Dropbox
        if (url.contains("dropbox.com")) {
            return when {
                url.contains("dl=0") -> url.replace("dl=0", "dl=1")
                url.contains("dl=1") -> url
                url.contains("?") -> "$url&dl=1"
                else -> "$url?dl=1"
            }
        }
        return url
    }

    private fun driveDirect(id: String) =
        "https://drive.usercontent.google.com/download?id=$id&export=download"

    suspend fun fetch(link: String): String = withContext(Dispatchers.IO) {
        var current = resolve(link)
        var redirects = 0
        var result: String? = null
        while (result == null) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Soalin/1.1")
                setRequestProperty("Accept", "application/json,text/plain,*/*")
            }
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                        ?: throw IOException("Pengalihan link tanpa tujuan.")
                    current = URL(URL(current), loc).toString()
                    redirects++
                    if (redirects > 8) throw IOException("Terlalu banyak pengalihan link.")
                } else if (code != 200) {
                    throw IOException(
                        when (code) {
                            403, 401 -> "Akses ditolak (HTTP $code). Pastikan file dibagikan ke publik."
                            404 -> "File tidak ditemukan (HTTP 404). Cek lagi link-nya."
                            else -> "Gagal mengunduh (HTTP $code)."
                        }
                    )
                } else {
                    val out = ByteArrayOutputStream()
                    conn.inputStream.use { input ->
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            if (out.size() > MAX_BYTES) throw IOException("File terlalu besar (maks 5 MB).")
                        }
                    }
                    result = out.toString("UTF-8")
                }
            } finally {
                conn.disconnect()
            }
        }
        result!!
    }
}
