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
import kotlinx.coroutines.delay
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

    /** True jika layar pernah benar-benar tersemat selama sesi ini. */
    @Volatile
    var everPinned = false

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

    /**
     * Menunggu hasil dialog penyematan layar setelah pin().
     * true = tersemat, false = ditolak (atau dialog tidak muncul karena fitur mati di Pengaturan).
     */
    suspend fun awaitPinned(activity: Activity): Boolean {
        // 1) Dialog sistem harus muncul (jendela aplikasi kehilangan fokus) atau langsung tersemat
        var waited = 0
        while (activity.hasWindowFocus() && !isPinned(activity)) {
            if (waited >= 2500) return false
            delay(100)
            waited += 100
        }
        // 2) Dialog tampil: tunggu pengguna memilih
        while (!activity.hasWindowFocus() && !isPinned(activity)) delay(100)
        // 3) Fokus kembali: beri jeda singkat sampai status sematan ter-update
        var grace = 0
        while (!isPinned(activity) && grace < 1000) {
            delay(100)
            grace += 100
        }
        return isPinned(activity)
    }

    fun isPinned(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }
}

/**
 * Suara bel alarm (dibuat lewat kode, tanpa file suara) lewat jalur volume alarm.
 * - ring(): 5 detik (selesai / keluar lewat tombol aplikasi)
 * - ringForever(): terus-menerus sampai HP dihidupkan ulang (keluar paksa saat tersemat)
 */
object BellPlayer {
    private const val RATE = 22050
    private const val SECONDS = 5
    private var busyUntil = 0L
    private var current: AudioTrack? = null
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    var forever = false
        private set

    fun maxAlarmVolume(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) < max) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
            }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun ring(context: Context) {
        if (forever) return
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
            val track = createTrack(samples)
            current = track
            track.play()
            handler.postDelayed({
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
                if (current === track) current = null
            }, SECONDS * 1000L + 300L)
        } catch (_: Exception) {
            busyUntil = 0L
        }
    }

    /** Bunyi terus-menerus. Tidak ada fungsi untuk menghentikannya: hanya restart HP. */
    @Synchronized
    fun ringForever(context: Context) {
        if (forever) return
        forever = true
        handler.removeCallbacksAndMessages(null)
        try {
            current?.stop()
            current?.release()
        } catch (_: Exception) {
        }
        maxAlarmVolume(context)
        try {
            val samples = buildBell()
            val track = createTrack(samples)
            track.setLoopPoints(0, samples.size, -1)
            current = track
            track.play()
        } catch (_: Exception) {
            forever = false
        }
    }

    private fun createTrack(samples: ShortArray): AudioTrack {
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
        return track
    }

    /** Bel dipukul 2 kali per detik; panjang total 5 detik (bisa diulang tanpa jeda). */
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
