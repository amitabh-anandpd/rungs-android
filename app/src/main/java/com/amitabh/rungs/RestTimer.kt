package com.amitabh.rungs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.flow.MutableStateFlow

/** Rest countdown. [endsAt] is the single source of truth for both the screen and the service. */
object RestTimer {
    val endsAt = MutableStateFlow<Long?>(null)

    fun start(ctx: Context, seconds: Int) = set(ctx, System.currentTimeMillis() + seconds * 1000L)

    fun add(ctx: Context, seconds: Int) = endsAt.value?.let { set(ctx, it + seconds * 1000L) }

    fun cancel(ctx: Context) {
        endsAt.value = null
        ctx.stopService(Intent(ctx, RestService::class.java))
    }

    private fun set(ctx: Context, end: Long) {
        endsAt.value = end
        ctx.startForegroundService(Intent(ctx, RestService::class.java))
    }
}

/**
 * Keeps the countdown alive with the phone locked and vibrates at 0:00. A partial wake lock is held
 * only for the rest itself; without it the CPU sleeps and the vibration arrives late.
 * ponytail: shortService caps one run at ~3 min, so a rest stretched past that with +15 s ends silently.
 */
class RestService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var wake: PowerManager.WakeLock? = null
    private val done = Runnable {
        vibrate(this)
        RestTimer.endsAt.value = null
        stop()
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val end = RestTimer.endsAt.value ?: System.currentTimeMillis()
        // Must be called promptly after startForegroundService, even if we stop right away.
        ServiceCompat.startForeground(
            this, 1, notification(end),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else 0,
        )
        val left = end - System.currentTimeMillis()
        if (left <= 0) {
            stop()
            return START_NOT_STICKY
        }
        wake?.release()
        wake = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rungs:rest")
            .apply { acquire(left + 5_000) }
        handler.removeCallbacks(done)
        handler.postDelayed(done, left)
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) = stop()

    override fun onDestroy() {
        handler.removeCallbacks(done)
        wake?.takeIf { it.isHeld }?.release()
    }

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(end: Long): android.app.Notification {
        val nm = getSystemService(NotificationManager::class.java)
        // Low importance: no sound, per the brief. The vibration comes from vibrate() at 0:00.
        nm.createNotificationChannel(NotificationChannel("rest", "Rest timer", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, "rest")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Rest")
            .setContentText("Phone vibrates at 0:00")
            .setWhen(end)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .build()
    }
}

fun vibrate(ctx: Context) {
    val v = if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java).defaultVibrator
    else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)
    val effect = VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1)
    // Alarm usage so it still buzzes with the ringer on silent.
    if (Build.VERSION.SDK_INT >= 33) v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
    else v.vibrate(effect)
}
