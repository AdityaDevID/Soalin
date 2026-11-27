package com.soalin.app.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class QuizParseException(message: String) : Exception(message)

object QuizParser {

    fun parse(raw: String, source: String?): QuizSet {
        val text = raw.trim().removePrefix("\uFEFF").trim()
        if (text.isEmpty()) throw QuizParseException("File soal kosong.")
        if (text.startsWith("<")) {
            throw QuizParseException(
                "Link mengarah ke halaman web, bukan file soal. " +
                    "Pastikan file dibagikan ke publik (Anyone with the link) dan link-nya benar."
            )
        }

        val root: JSONObject = try {
            if (text.startsWith("[")) JSONObject().put("soal", JSONArray(text)) else JSONObject(text)
        } catch (e: JSONException) {
            throw QuizParseException("Format JSON tidak valid: ${e.message}")
        }

        val arr = root.optJSONArray("soal") ?: root.optJSONArray("questions")
            ?: throw QuizParseException("Field \"soal\" tidak ditemukan di file.")
        if (arr.length() == 0) throw QuizParseException("Daftar soal kosong.")

        val questions = ArrayList<Question>()
        for (i in 0 until arr.length()) {
            val no = i + 1
            val o = arr.optJSONObject(i) ?: throw QuizParseException("Soal nomor $no bukan objek JSON.")

            val q = o.optString("pertanyaan", o.optString("question", "")).trim()
            if (q.isEmpty()) throw QuizParseException("Soal nomor $no: \"pertanyaan\" kosong.")

            val optArr = o.optJSONArray("pilihan") ?: o.optJSONArray("options")
                ?: throw QuizParseException("Soal nomor $no: \"pilihan\" tidak ada.")
            val options = (0 until optArr.length()).map { optArr.optString(it).trim() }
            if (options.size < 2) throw QuizParseException("Soal nomor $no: minimal 2 pilihan.")
            if (options.any { it.isEmpty() }) throw QuizParseException("Soal nomor $no: ada pilihan yang kosong.")

            val ansRaw = if (o.has("jawaban")) o.get("jawaban") else if (o.has("answer")) o.get("answer") else null
            val answerIndex = parseAnswer(ansRaw, options, no)

            val expl = o.optString("pembahasan", o.optString("explanation", "")).trim().ifEmpty { null }
            questions.add(Question(q, options, answerIndex, expl))
        }

        val title = root.optString("judul", root.optString("title", "")).trim().ifEmpty { "Soal tanpa judul" }
        val desc = root.optString("deskripsi", root.optString("description", "")).trim()
        val src = source ?: root.optString("sumber", "").ifEmpty { null }
        return QuizSet(idFromTitle(title), title, desc, questions, src)
    }

    private fun parseAnswer(raw: Any?, options: List<String>, no: Int): Int {
        val idx: Int = when (raw) {
            null, JSONObject.NULL -> throw QuizParseException("Soal nomor $no: \"jawaban\" tidak ada.")
            is Number -> raw.toInt() - 1 // angka dihitung mulai dari 1
            else -> {
                val s = raw.toString().trim()
                when {
                    s.length == 1 && s[0].isLetter() -> s[0].uppercaseChar() - 'A'
                    s.toIntOrNull() != null -> s.toInt() - 1
                    else -> options.indexOfFirst { it.equals(s, ignoreCase = true) }
                }
            }
        }
        if (idx !in options.indices) {
            throw QuizParseException("Soal nomor $no: \"jawaban\" tidak cocok dengan pilihan yang ada.")
        }
        return idx
    }

    fun idFromTitle(title: String): String =
        "s" + Integer.toHexString(title.trim().lowercase().hashCode())

    fun toJson(set: QuizSet): String {
        val root = JSONObject()
        root.put("judul", set.title)
        root.put("deskripsi", set.description)
        set.source?.let { root.put("sumber", it) }
        val arr = JSONArray()
        for (q in set.questions) {
            val o = JSONObject()
            o.put("pertanyaan", q.text)
            o.put("pilihan", JSONArray(q.options))
            o.put("jawaban", ('A' + q.answerIndex).toString())
            q.explanation?.let { o.put("pembahasan", it) }
            arr.put(o)
        }
        root.put("soal", arr)
        return root.toString()
    }
}
