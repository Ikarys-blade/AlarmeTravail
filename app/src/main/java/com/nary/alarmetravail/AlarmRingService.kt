package com.nary.alarmetravail

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

/**
 * Service au premier plan qui sonne + vibre + affiche une notification plein
 * écran quand une alarme se déclenche. Reste actif tant que l'utilisateur n'a
 * pas appuyé sur "Arrêter" ou "J'ai terminé le travail".
 */
class AlarmRingService : Service() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "alarm_ring_channel"
        const val NOTIF_ID_BASE = 5000
        const val ACTION_STOP = "com.nary.alarmetravail.ACTION_STOP"
        const val ACTION_DONE = "com.nary.alarmetravail.ACTION_DONE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"

        var currentRingingId: Long = -1L
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val id = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                stopRinging(id, resumeCountdown = false)
                return START_NOT_STICKY
            }
            ACTION_DONE -> {
                val id = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                stopRinging(id, resumeCountdown = true)
                return START_NOT_STICKY
            }
            else -> {
                val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
                if (alarmId == -1L) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startRinging(alarmId)
                return START_STICKY
            }
        }
    }

    private fun startRinging(alarmId: Long) {
        val profile = AlarmStore.findById(this, alarmId) ?: run {
            stopSelf()
            return
        }
        currentRingingId = alarmId

        createChannel()

        // Intent pour ouvrir l'écran plein écran de sonnerie
        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarmId.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("C'est l'heure de travailler")
            .setContentText(profile.personName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIF_ID_BASE + alarmId.toInt(), notification)

        // Ouvre aussi directement l'activité plein écran (utile si l'écran est allumé)
        startActivity(fullScreenIntent)

        playSound()
        vibrate()
    }

    private fun playSound() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            ringtone?.play()
        } catch (e: Exception) {
            // Si aucune sonnerie n'est disponible, on continue avec juste la vibration
        }
    }

    private fun vibrate() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, 0)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopRinging(alarmId: Long, resumeCountdown: Boolean) {
        ringtone?.stop()
        vibrator?.cancel()

        val id = if (alarmId != -1L) alarmId else currentRingingId
        val profile = AlarmStore.findById(this, id)
        if (profile != null && resumeCountdown) {
            // "J'ai terminé le travail" -> on relance le compte à rebours de 3h
            AlarmScheduler.scheduleFromNow(this, profile)
        }
        // Si "Arrêter" simple : on ne reprogramme rien ici, l'alarme suivante
        // aura déjà été programmée normalement par le flux habituel si isEnabled reste true.
        // On reprogramme quand même la prochaine échéance pour que le cycle continue.
        if (profile != null && !resumeCountdown && profile.isEnabled) {
            AlarmScheduler.scheduleFromNow(this, profile)
        }

        currentRingingId = -1L
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID_BASE + id.toInt())

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alarmes de travail",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications plein écran pour les rappels de travail"
                    enableVibration(true)
                    setSound(null, null) // le son est géré manuellement via Ringtone
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
        vibrator?.cancel()
    }
}