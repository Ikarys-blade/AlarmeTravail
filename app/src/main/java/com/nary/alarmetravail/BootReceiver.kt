package com.nary.alarmetravail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Après un redémarrage du téléphone, toutes les alarmes système sont perdues.
 * On les reprogramme ici à partir des données sauvegardées.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val alarms = AlarmStore.loadAll(context)
        val now = System.currentTimeMillis()

        alarms.forEach { profile ->
            if (profile.isEnabled) {
                // Si l'heure prévue est déjà passée, on repart sur un intervalle complet
                val triggerAt = if (profile.nextTriggerAt > now) {
                    profile.nextTriggerAt
                } else {
                    now + profile.intervalMillis
                }
                profile.nextTriggerAt = triggerAt
                AlarmStore.update(context, profile)
                AlarmScheduler.schedule(context, profile, triggerAt)
            }
        }
    }
}