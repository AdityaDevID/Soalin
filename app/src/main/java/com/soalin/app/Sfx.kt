package com.soalin.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Efek suara jawaban (dibuat lewat kode, tanpa file suara):
 * - benar: dua nada naik, bunyi "ting-ting" ceria
 * - salah: dua nada turun, bunyi "tuut-tut" rendah
 * Lewat volume media, jadi mengikuti volume HP.
 */
object Sfx {
    private const val RATE = 44100
    private val main = Handler(Looper.getMainLooper())

    private val correct: ShortArray by lazy {
        mix(
            0.50,
            bell(freq = 988.0, start = 0.00, len = 0.28),   // B5
            bell(freq = 1318.5, start = 0.11, len = 0.40)   // E6
        )
    }

    private val wrong: ShortArray by lazy {
        mix(
            0.45,
            buzz(freq = 233.0, start = 0.00, len = 0.20),   // A#3
            buzz(freq = 174.6, start = 0.17, len = 0.32)    // F3
        )
    }

    fun play(isCorrect: Boolean) {
        try {
            val data = if (isCorrect) correct else wrong
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(data, 0, data.size)
            track.play()
            val ms = data.size * 1000L / RATE + 150
            main.postDelayed({
                try {
                    track.stop()
                } catch (_: Exception) {
                }
                track.release()
            }, ms)
        } catch (_: Exception) {
        }
    }

    // Nada seperti lonceng: nada dasar + harmonik, peluruhan eksponensial
    private fun bell(freq: Double, start: Double, len: Double): Pair<Int, DoubleArray> {
        val n = (len * RATE).toInt()
        val out = DoubleArray(n)
        val ratios = doubleArrayOf(1.0, 2.0, 3.0, 4.2)
        val amps = doubleArrayOf(1.0, 0.45, 0.2, 0.08)
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val attack = min(1.0, t / 0.004)
            val release = min(1.0, (len - t) / 0.03)
            var s = 0.0
            for (k in ratios.indices) s += amps[k] * sin(2 * PI * freq * ratios[k] * t) * exp(-t * (7.0 + 5.0 * k))
            out[i] = s * attack * release.coerceAtLeast(0.0)
        }
        return (start * RATE).toInt() to out
    }

    // Nada rendah kasar (gelombang gigi gergaji lunak) untuk jawaban salah
    private fun buzz(freq: Double, start: Double, len: Double): Pair<Int, DoubleArray> {
        val n = (len * RATE).toInt()
        val out = DoubleArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val attack = min(1.0, t / 0.006)
            val release = min(1.0, (len - t) / 0.05)
            var s = 0.0
            for (h in 1..6) s += sin(2 * PI * freq * h * t) / h
            out[i] = s * 0.6 * attack * release.coerceAtLeast(0.0) * exp(-t * 3.0)
        }
        return (start * RATE).toInt() to out
    }

    private fun mix(gain: Double, vararg parts: Pair<Int, DoubleArray>): ShortArray {
        val total = parts.maxOf { it.first + it.second.size }
        val buf = DoubleArray(total)
        for ((off, d) in parts) for (i in d.indices) buf[off + i] += d[i]
        val peak = buf.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-9)
        return ShortArray(total) { (buf[it] / peak * gain * Short.MAX_VALUE).toInt().toShort() }
    }
}
