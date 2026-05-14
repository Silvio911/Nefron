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
        val oldDuration = durations[index]
        if (minutes == oldDuration) return

        val totalMinutes = END_HOUR * 60 - (START_HOUR * 60 + START_MINUTE)
        val prefs = context.getSharedPreferences(PREFS_PHONES, Context.MODE_PRIVATE)

        if (minutes < oldDuration) {
            // Shrink: insert leftover as a new free slot right after,
            // shifting phone bookings up by 1 so their start times don't move.
            val leftover = oldDuration - minutes
            durations[index] = minutes
            durations.add(index + 1, leftover)

            val editor = prefs.edit()
            for (i in 25 downTo (index + 1)) {
                val phone = prefs.getString("$day.$i", null)
                if (phone != null) editor.putString("$day.${i + 1}", phone)
                else editor.remove("$day.${i + 1}")
            }
            editor.remove("$day.${index + 1}") // inserted slot is free
            editor.apply()

            var used = 0
            val newDurations = mutableListOf<Int>()
            for (d in durations) {
                if (used + d > totalMinutes) break
                newDurations.add(d)
                used += d
            }
            saveDurations(context, day, newDurations)
        } else {
            // Expand: absorb time from the immediately following free slot only.
            val delta = minutes - oldDuration
            val nextIndex = index + 1
            if (nextIndex >= durations.size) return
            val nextDuration = durations[nextIndex]
            if (nextDuration < delta) return // next slot too small

            durations[index] = minutes

            if (nextDuration == delta) {
                // Fully absorb next slot: remove it and shift phone indices down by 1
                durations.removeAt(nextIndex)
                val editor = prefs.edit()
                for (i in nextIndex until 25) {
                    val phone = prefs.getString("$day.${i + 1}", null)
                    if (phone != null) editor.putString("$day.$i", phone)
                    else editor.remove("$day.$i")
                }
                editor.apply()
            } else {
                // Partial absorption: shrink next slot by delta, indices unchanged
                durations[nextIndex] = nextDuration - delta
            }

            saveDurations(context, day, durations)
        }
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
