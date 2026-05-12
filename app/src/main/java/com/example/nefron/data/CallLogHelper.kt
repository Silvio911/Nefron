package com.example.nefron.data

import android.content.Context
import android.provider.CallLog

object CallLogHelper {
    fun getLastIncomingCall(context: Context): String? {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER),
            "${CallLog.Calls.TYPE} = ?",
            arrayOf(CallLog.Calls.INCOMING_TYPE.toString()),
            "${CallLog.Calls.DATE} DESC"
        ) ?: return null
        return cursor.use { if (it.moveToFirst()) it.getString(0) else null }
    }
}
