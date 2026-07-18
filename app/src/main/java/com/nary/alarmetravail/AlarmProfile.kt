package com.nary.alarmetravail

import org.json.JSONObject

/**
 * Représente une alarme récurrente associée à une personne
 * (ex: "Papa", "Moi", "Rina"...).
 *
 * intervalMillis : durée entre deux sonneries (par défaut 3h)
 * nextTriggerAt  : timestamp (epoch millis) du prochain déclenchement
 * isEnabled      : l'alarme est active ou non
 * isPaused       : vrai pendant que la personne "travaille" (alarme en pause,
 *                   reprend seulement quand on appuie sur "J'ai terminé")
 */
data class AlarmProfile(
    val id: Long,
    var personName: String,
    var intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    var nextTriggerAt: Long = 0L,
    var isEnabled: Boolean = true,
    var colorHex: String = "#3DDC84"
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("personName", personName)
        o.put("intervalMillis", intervalMillis)
        o.put("nextTriggerAt", nextTriggerAt)
        o.put("isEnabled", isEnabled)
        o.put("colorHex", colorHex)
        return o
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 3 * 60 * 60 * 1000L // 3 heures

        fun fromJson(o: JSONObject): AlarmProfile {
            return AlarmProfile(
                id = o.getLong("id"),
                personName = o.getString("personName"),
                intervalMillis = o.optLong("intervalMillis", DEFAULT_INTERVAL_MILLIS),
                nextTriggerAt = o.optLong("nextTriggerAt", 0L),
                isEnabled = o.optBoolean("isEnabled", true),
                colorHex = o.optString("colorHex", "#3DDC84")
            )
        }
    }
}