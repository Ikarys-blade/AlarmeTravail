package com.nary.alarmetravail

import android.content.Context
import org.json.JSONArray

/**
 * Sauvegarde/charge la liste des AlarmProfile en local (SharedPreferences).
 * Simple, pas de dépendance externe, suffisant pour une poignée d'alarmes.
 */
object AlarmStore {
    private const val PREFS = "alarme_travail_prefs"
    private const val KEY_ALARMS = "alarms_json"
    private const val KEY_NEXT_ID = "next_id"

    fun loadAll(context: Context): MutableList<AlarmProfile> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ALARMS, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val list = mutableListOf<AlarmProfile>()
        for (i in 0 until arr.length()) {
            list.add(AlarmProfile.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    fun saveAll(context: Context, alarms: List<AlarmProfile>) {
        val arr = JSONArray()
        alarms.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALARMS, arr.toString())
            .apply()
    }

    fun nextId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_NEXT_ID, 1L)
        prefs.edit().putLong(KEY_NEXT_ID, id + 1).apply()
        return id
    }

    fun findById(context: Context, id: Long): AlarmProfile? {
        return loadAll(context).find { it.id == id }
    }

    fun update(context: Context, updated: AlarmProfile) {
        val list = loadAll(context)
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            list[idx] = updated
        } else {
            list.add(updated)
        }
        saveAll(context, list)
    }

    fun delete(context: Context, id: Long) {
        val list = loadAll(context)
        list.removeAll { it.id == id }
        saveAll(context, list)
    }
}