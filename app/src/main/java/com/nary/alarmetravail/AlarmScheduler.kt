package com.nary.alarmetravail

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Programme ou annule les alarmes exactes du système (AlarmManager) pour
 * un profil donné. Utilise setExactAndAllowWhileIdle pour que ça sonne
 * même en mode Doze / app fermée.
 */
object AlarmScheduler {

    private fun pendingIntent(context: Context, profile: AlarmProfile): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, profile.id)
        }
        return PendingIntent.getBroadcast(
            context,
            profile.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Programme le prochain déclenchement à triggerAtMillis. */
    fun schedule(context: Context, profile: AlarmProfile, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, profile)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Permission pas encore accordée : on programme quand même une alarme
            // inexacte pour ne pas planter ; MainActivity redirige vers les réglages.
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pi
        )
    }

    /** Reprogramme selon l'intervalle du profil à partir de maintenant. */
    fun scheduleFromNow(context: Context, profile: AlarmProfile) {
        val triggerAt = System.currentTimeMillis() + profile.intervalMillis
        profile.nextTriggerAt = triggerAt
        AlarmStore.update(context, profile)
        schedule(context, profile, triggerAt)
    }

    fun cancel(context: Context, profile: AlarmProfile) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, profile))
    }
}