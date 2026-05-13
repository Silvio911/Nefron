package com.nefron.app.data

import android.content.Context

object SlotStorage {

    private const val PREFS_PHONES     = "clinic_phones"
    private const val PREFS_DURATIONS  = "clinic_durations"
    private const val START_HOUR       = 16
    private const val START_MINUTE     = 30
    private const val END_HOUR         = 21
    private const val DEFAULT_DURATION = 30

    val DAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")

    fun getSlotsForDay(context: Context, day: String): List<Slot> {
        val durations = getDurations(context, day)
        val slots = mutableListOf<Slot>()
        var minutesFromStart = 0
        val totalMinutes = END_HOUR * 60 - (START_HOUR * 60 + START_MINUTE)
        for ((index, duration) in durations.withIndex()) {
            if (minutesFromStart + duration > totalMinutes) break
            slots.add(Slot(index, minutesToTime(minutesFromStart), duration, getPhone(context, day, index)))
            minutesFromStart += duration
        }
        return slots
    }

    fun setDuration(context: Context, day: String, index: Int, minutes: Int) {
        val durations = getDurations(context, day).toMutableList()
        if (index >= durations.size) return
        durations[index] = minutes

        val totalMinutes = END_HOUR * 60 - (START_HOUR * 60 + START_MINUTE)
        var used = 0
        val newDurations = mutableListOf<Int>()
        for (d in durations) {
            if (used + d > totalMinutes) break
            newDurations.add(d)
            used += d
        }
        // remove phones for slots that were trimmed
        val editor = context.getSharedPreferences(PREFS_PHONES, Context.MODE_PRIVATE).edit()
        for (i in newDurations.size until durations.size + 5) editor.remove("$day.$i")
        // fill remaining room with default slots
        while (used + DEFAULT_DURATION <= totalMinutes) {
            newDurations.add(DEFAULT_DURATION)
            used += DEFAULT_DURATION
        }
        editor.apply()
        saveDurations(context, day, newDurations)
    }

    fun getPhone(context: Context, day: String, index: Int): String? =
        context.getSharedPreferences(PREFS_PHONES, Context.MODE_PRIVATE)
            .getString("$day.$index", null)

    fun setPhone(context: Context, day: String, index: Int, phone: String) =
        context.getSharedPreferences(PREFS_PHONES, Context.MODE_PRIVATE)
            .edit().putString("$day.$index", phone).apply()

    fun clearPhone(context: Context, day: String, index: Int) =
        context.getSharedPreferences(PREFS_PHONES, Context.MODE_PRIVATE)
            .edit().remove("$day.$index").apply()

    fun resetAll(context: Context) {
        context.getSharedPreferences(PREFS_PHONES, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences(PREFS_DURATIONS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun getDurations(context: Context, day: String): List<Int> {
        val stored = context.getSharedPreferences(PREFS_DURATIONS, Context.MODE_PRIVATE)
            .getString(day, null)
        if (stored != null) return stored.split(",").mapNotNull { it.toIntOrNull() }
        val defaults = List((END_HOUR * 60 - (START_HOUR * 60 + START_MINUTE)) / DEFAULT_DURATION) { DEFAULT_DURATION }
        saveDurations(context, day, defaults)
        return defaults
    }

    private fun saveDurations(context: Context, day: String, durations: List<Int>) =
        context.getSharedPreferences(PREFS_DURATIONS, Context.MODE_PRIVATE)
            .edit().putString(day, durations.joinToString(",")).apply()

    private fun minutesToTime(minutes: Int): String {
        val total = START_HOUR * 60 + START_MINUTE + minutes
        return "%02d:%02d".format(total / 60, total % 60)
    }
}
