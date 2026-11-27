package com.soalin.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Service latar depan yang aktif selama sesi soal. Tugasnya:
 * - menjaga proses tetap hidup saat aplikasi ditinggalkan
 * - memantau penyematan layar; jika sempat tersemat lalu terlepas di tengah sesi, bel berbunyi terus
 * - setelah bel terus-menerus aktif, volume alarm dijaga tetap maksimal
 */
class GuardService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            if (BellPlayer.forever) {
                BellPlayer.maxAlarmVolume(this@GuardService)
            } else if (Guard.quizActive) {
                if (Guard.isPinned(this@GuardService)) {
                    Guard.everPinned = true
                } else if (Guard.everPinned) {
                    BellPlayer.ringForever(this@GuardService)
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Sesi belajar", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Soalin")
            .setContentText("Sesi belajar sedang berlangsung")
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "sesi_belajar"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, GuardService::class.java))
        }

        /** Hentikan service, kecuali bel terus-menerus sedang aktif (itu hanya berhenti saat HP restart). */
        fun stop(context: Context) {
            if (!BellPlayer.forever) context.stopService(Intent(context, GuardService::class.java))
        }
    }
}
