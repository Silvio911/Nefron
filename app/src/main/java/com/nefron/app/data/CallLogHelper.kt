package com.nefron.app.data

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract

object CallLogHelper {

    fun getLastIncomingCallDisplay(context: Context): String? {
        val number = getLastIncomingNumber(context) ?: return null
        return lookupContactName(context, number) ?: number
    }

    private fun getLastIncomingNumber(context: Context): String? {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER),
            "${CallLog.Calls.TYPE} = ?",
            arrayOf(CallLog.Calls.INCOMING_TYPE.toString()),
            "${CallLog.Calls.DATE} DESC"
        ) ?: return null
        return cursor.use { if (it.moveToFirst()) it.getString(0) else null }
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
            null // permission not granted yet — fall back to number
        }
    }
}
