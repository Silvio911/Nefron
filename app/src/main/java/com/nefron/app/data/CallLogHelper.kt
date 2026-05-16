package com.nefron.app.data

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CallEntry(
    val name: String?,    // contact name, null if unknown
    val number: String,   // raw phone number
    val time: String      // formatted call time
) {
    val display get() = name ?: number
}

object CallLogHelper {

    fun getRecentIncomingCalls(context: Context): List<CallEntry> {
        val entries = mutableListOf<CallEntry>()
        val seen = mutableSetOf<String>()
        val todayStart = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} != ${CallLog.Calls.OUTGOING_TYPE}",
                null,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return emptyList()
            cursor.use {
                while (it.moveToNext()) {
                    val number = it.getString(0)?.takeIf { n -> n.isNotBlank() } ?: continue
                    val date   = it.getLong(1)
                    if (date < todayStart && entries.size >= 30) break
                    if (seen.add(number)) {
                        entries.add(
                            CallEntry(
                                name   = lookupContactName(context, number),
                                number = number,
                                time   = formatTime(date)
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return entries
    }

    private fun formatTime(timestamp: Long): String {
        val zone = ZoneId.systemDefault()
        val dt   = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDateTime()
        return if (dt.toLocalDate() == LocalDate.now(zone)) {
            dt.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            dt.format(DateTimeFormatter.ofPattern("dd MMM  HH:mm"))
        }
    }

    private fun lookupContactName(context: Context, number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            ) ?: return null
            cursor.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: SecurityException) {
            null
        }
    }
}
