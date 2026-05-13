package com.nefron.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.nefron.app.data.CallLogHelper
import com.nefron.app.data.SlotStorage
import com.nefron.app.widget.ClinicWidget
import java.util.Calendar
import kotlinx.coroutines.launch

@Composable
fun ScheduleScreen() {
    val isSunday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    if (isSunday) {
        ClosedScreen()
        return
    }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var selectedDay     by remember { mutableStateOf(todayLabel()) }
    var slots           by remember { mutableStateOf(SlotStorage.getAllForDay(context, selectedDay)) }
    var dialogSlot      by remember { mutableStateOf<String?>(null) }
    var dialogInput     by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    fun refresh() {
        slots = SlotStorage.getAllForDay(context, selectedDay)
        scope.launch { ClinicWidget().updateAll(context) }
    }

    fun openDialog(time: String) {
        val lastCall = CallLogHelper.getLastIncomingCall(context) ?: ""
        val alreadyBooked = lastCall.isNotBlank() && slots.values.any { it == lastCall }
        val number = if (alreadyBooked) "" else lastCall
        if (number.isBlank()) {
            SlotStorage.setPhone(context, selectedDay, time, "")
            refresh()
        } else {
            dialogInput = number
            dialogSlot  = time
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { showResetDialog = true },
                containerColor   = Color(0xFF6B9DC2)
            ) {
                Icon(
                    imageVector        = Icons.Default.Refresh,
                    contentDescription = "Reset week",
                    tint               = Color.White
                )
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            ScrollableTabRow(selectedTabIndex = SlotStorage.DAYS.indexOf(selectedDay)) {
                SlotStorage.DAYS.forEach { day ->
                    Tab(
                        selected = day == selectedDay,
                        onClick  = { selectedDay = day; slots = SlotStorage.getAllForDay(context, day) },
                        text     = { Text(day, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SlotStorage.SLOTS) { time ->
                    SlotRow(
                        time  = time,
                        phone = slots[time],
                        onTap = { openDialog(time) }
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset week?") },
            text  = { Text("This will clear all booked slots for every day. Cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    SlotStorage.resetAll(context)
                    refresh()
                    showResetDialog = false
                }) { Text("Reset", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    dialogSlot?.let { time ->
        AlertDialog(
            onDismissRequest = { dialogSlot = null },
            title = { Text("$selectedDay $time") },
            text  = {
                OutlinedTextField(
                    value           = dialogInput,
                    onValueChange   = { dialogInput = it },
                    label           = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    SlotStorage.setPhone(context, selectedDay, time, dialogInput.trim())
                    refresh()
                    dialogSlot = null
                }) { Text("Book") }
            },
            dismissButton = {
                TextButton(onClick = { dialogSlot = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ClosedScreen() {
    Box(
        modifier         = Modifier.fillMaxSize().statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text       = "Closed",
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF6B9DC2)
            )
            Text(
                text      = "The clinic is closed on Sundays.\nSee you tomorrow.",
                fontSize  = 16.sp,
                color     = Color(0xFF8AA0B0),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SlotRow(time: String, phone: String?, onTap: () -> Unit) {
    val assigned = phone != null
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        colors   = CardDefaults.cardColors(
            containerColor = if (assigned) MaterialTheme.colorScheme.primaryContainer
                             else Color(0xFFF0F4F8)
        )
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = time,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = if (assigned) MaterialTheme.colorScheme.onPrimaryContainer
                             else Color(0xFF4A6070),
                modifier   = Modifier.width(72.dp)
            )
            Text(
                text     = phone?.ifBlank { "Booked" } ?: "available",
                fontSize = 16.sp,
                color    = if (assigned) MaterialTheme.colorScheme.onPrimaryContainer
                           else Color(0xFF8AA0B0),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun todayLabel() = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY   -> "MON"
    Calendar.TUESDAY  -> "TUE"
    Calendar.WEDNESDAY -> "WED"
    Calendar.THURSDAY -> "THU"
    Calendar.FRIDAY   -> "FRI"
    else              -> "SAT"
}
