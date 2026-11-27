package com.soalin.app.data

import android.content.Context
import java.io.File

/** Catatan sesi terakhir sebuah paket soal. */
data class SessionLog(val time: Long, val correct: Int, val wrong: Int)

class Repository(context: Context) {

    private val dir = File(context.filesDir, "soal").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("skor", Context.MODE_PRIVATE)

    fun loadAll(): List<QuizSet> =
        (dir.listFiles { f -> f.extension == "json" } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .mapNotNull { f -> runCatching { QuizParser.parse(f.readText(), null) }.getOrNull() }

    /** Menyimpan paket soal. Judul yang sama akan menimpa paket lama (jadi bisa di-update). */
    fun save(set: QuizSet) {
        File(dir, "${set.id}.json").writeText(QuizParser.toJson(set))
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
        prefs.edit().remove("best_$id").remove("log_time_$id").remove("log_ok_$id").remove("log_bad_$id").apply()
    }

    /** Menyimpan catatan sesi terakhir (dipanggil tiap jawaban dikunci, jadi tetap ada walau sesi ditinggalkan). */
    fun saveLog(id: String, time: Long, correct: Int, wrong: Int) {
        prefs.edit()
            .putLong("log_time_$id", time)
            .putInt("log_ok_$id", correct)
            .putInt("log_bad_$id", wrong)
            .apply()
    }

    fun lastLog(id: String): SessionLog? {
        val t = prefs.getLong("log_time_$id", 0L)
        if (t == 0L) return null
        return SessionLog(t, prefs.getInt("log_ok_$id", 0), prefs.getInt("log_bad_$id", 0))
    }

    fun bestScore(id: String): Int = prefs.getInt("best_$id", -1)

    fun saveBestScore(id: String, percent: Int) {
        if (percent > bestScore(id)) prefs.edit().putInt("best_$id", percent).apply()
    }
}
