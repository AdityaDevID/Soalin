package com.soalin.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Efek suara jawaban dari file di res/raw:
 * - sfx_correct.mp3: jawaban benar
 * - sfx_wrong.mp3: jawaban salah
 * Lewat volume media, jadi mengikuti volume HP.
 */
object Sfx {
    private var pool: SoundPool? = null
    private var correctId = 0
    private var wrongId = 0
    private val ready = HashSet<Int>()

    /** Muat suara lebih dulu supaya langsung bunyi saat tombol Jawab ditekan. */
    @Synchronized
    fun load(context: Context) {
        if (pool != null) return
        val app = context.applicationContext
        val sp = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        sp.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(this) { ready.add(id) } }
        correctId = sp.load(app, R.raw.sfx_correct, 1)
        wrongId = sp.load(app, R.raw.sfx_wrong, 1)
        pool = sp
    }

    @Synchronized
    fun play(isCorrect: Boolean) {
        try {
            val id = if (isCorrect) correctId else wrongId
            if (id in ready) pool?.play(id, 1f, 1f, 1, 0, 1f)
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun release() {
        pool?.release()
        pool = null
        ready.clear()
    }
}
