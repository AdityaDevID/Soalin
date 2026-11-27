package com.soalin.app

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Penyematan layar (screen pinning) selama sesi soal berlangsung. */
object Guard {
    @Volatile
    var quizActive = false

    fun pin(activity: Activity) {
        try {
            activity.startLockTask()
        } catch (_: Exception) {
        }
    }

    fun unpin(activity: Activity) {
        try {
            activity.stopLockTask()
        } catch (_: Exception) {
        }
    }

    fun isPinned(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }
}

/** Bunyi bel alarm 5 detik (dibuat lewat kode, tanpa file suara), keras lewat jalur volume alarm. */
object BellPlayer {
    private const val RATE = 22050
    private const val SECONDS = 5
    private var busyUntil = 0L
    private val handler = Handler(Looper.getMainLooper())

    @Synchronized
    fun ring(context: Context) {
        val now = SystemClock.elapsedRealtime()
        if (now < busyUntil) return // sedang berbunyi, jangan dobel
        busyUntil = now + (SECONDS * 1000L) + 500L

        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val wanted = (max * 0.7).toInt()
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) < wanted) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, wanted, 0)
            }
        } catch (_: Exception) {
        }

        try {
            val samples = buildBell()
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            handler.postDelayed({
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
            }, SECONDS * 1000L + 300L)
        } catch (_: Exception) {
            busyUntil = 0L
        }
    }

    /** Bel dipukul 2 kali per detik selama 5 detik. */
    private fun buildBell(): ShortArray {
        val out = ShortArray(RATE * SECONDS)
        val f0 = 660.0
        val ratios = doubleArrayOf(1.0, 2.0, 2.76, 5.4)
        val amps = doubleArrayOf(1.0, 0.6, 0.4, 0.2)
        val strike = RATE / 2
        for (i in out.indices) {
            val t = (i % strike) / RATE.toDouble()
            var v = 0.0
            for (k in ratios.indices) {
                v += amps[k] * sin(2 * PI * f0 * ratios[k] * t) * exp(-t * (3.0 + k * 2.0))
            }
            val attack = min(1.0, t / 0.003)
            val s = v * attack / 2.2 * 32767 * 0.95
            out[i] = s.toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }
}
