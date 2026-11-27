package com.soalin.app.data

import android.content.Context
import java.io.File

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
        prefs.edit().remove("best_$id").apply()
    }

    fun bestScore(id: String): Int = prefs.getInt("best_$id", -1)

    fun saveBestScore(id: String, percent: Int) {
        if (percent > bestScore(id)) prefs.edit().putInt("best_$id", percent).apply()
    }
}
