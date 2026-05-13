package com.nefron.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nefron.app.data.CallLogHelper
import com.nefron.app.data.Slot
import com.nefron.app.data.SlotStorage
import java.util.Calendar

private val keyDay   = ActionParameters.Key<String>("day")
private val keyIndex = ActionParameters.Key<Int>("index")

class ClinicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }
}

@Composable
private fun WidgetContent() {
    val context = LocalContext.current
    val today   = todayLabel()
    val slots   = SlotStorage.getSlotsForDay(context, today)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(8.dp)
    ) {
        Text(
            text  = today,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(4.dp))
        slots.forEach { slot ->
            WidgetSlotRow(slot = slot, today = today)
        }
    }
}

@Composable
private fun WidgetSlotRow(slot: Slot, today: String) {
    val baseModifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)
    val rowModifier  = if (slot.phone == null) {
        baseModifier.clickable(
            actionRunCallback<AssignSlotAction>(
                actionParametersOf(keyDay to today, keyIndex to slot.index)
            )
        )
    } else baseModifier

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text     = slot.startTime,
            style    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.width(54.dp)
        )
        Text(
            text  = slot.phone?.ifBlank { "Booked" } ?: "—",
            style = TextStyle(
                fontSize = 13.sp,
                color    = ColorProvider(if (slot.phone != null) Color(0xFF1B5E20) else Color.Gray)
            )
        )
    }
}

class AssignSlotAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val day   = parameters[keyDay]   ?: return
        val index = parameters[keyIndex] ?: return
        val phone = CallLogHelper.getLastIncomingCall(context) ?: return
        SlotStorage.setPhone(context, day, index, phone)
        ClinicWidget().update(context, glanceId)
    }
}

class ClinicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ClinicWidget()
}

private fun todayLabel() = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY    -> "MON"
    Calendar.TUESDAY   -> "TUE"
    Calendar.WEDNESDAY -> "WED"
    Calendar.THURSDAY  -> "THU"
    Calendar.FRIDAY    -> "FRI"
    else               -> "SAT"
}
