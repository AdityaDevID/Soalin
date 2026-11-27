package com.soalin.app.data

/** Satu soal asli (belum diacak). */
data class Question(
    val text: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String?
)

/** Satu paket soal yang tersimpan di HP. */
data class QuizSet(
    val id: String,
    val title: String,
    val description: String,
    val questions: List<Question>,
    val source: String?
)

/** Soal yang sudah diacak untuk satu sesi belajar. */
data class SessionQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String?
)

/** Setiap sesi baru: urutan soal diacak DAN urutan pilihan jawaban diacak. */
fun QuizSet.newSession(): List<SessionQuestion> =
    questions.shuffled().map { q ->
        val order = q.options.indices.shuffled()
        SessionQuestion(
            text = q.text,
            options = order.map { q.options[it] },
            correctIndex = order.indexOf(q.answerIndex),
            explanation = q.explanation
        )
    }
