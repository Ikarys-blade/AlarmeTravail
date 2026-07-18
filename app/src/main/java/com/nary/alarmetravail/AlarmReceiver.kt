package com.nary.alarmetravail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Reçu quand une alarme programmée par AlarmManager se déclenche.
 * Démarre le service de sonnerie plein écran pour le bon profil.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val profile = AlarmStore.findById(context, alarmId) ?: return
        if (!profile.isEnabled) return

        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}