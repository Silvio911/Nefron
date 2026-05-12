package com.nefron.app.data

import android.content.Context
import java.util.Calendar

object SlotStorage {

    private const val PREFS = "clinic_slots"
    private const val KEY_WEEK = "week_of_year"

    val DAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val SLOTS = listOf(
        "16:00", "16:30", "17:00", "17:30", "18:00",
        "18:30", "19:00", "19:30", "20:00", "20:30"
    )

    fun checkWeekReset(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        if (prefs.getInt(KEY_WEEK, -1) != currentWeek) {
            prefs.edit().clear().putInt(KEY_WEEK, currentWeek).apply()
        }
    }

    fun getPhone(context: Context, day: String, time: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("$day.$time", null)

    fun setPhone(context: Context, day: String, time: String, phone: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("$day.$time", phone).apply()

    fun clearPhone(context: Context, day: String, time: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove("$day.$time").apply()

    fun getAllForDay(context: Context, day: String): Map<String, String?> =
        SLOTS.associateWith { time -> getPhone(context, day, time) }
}
