package com.nefron.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nefron.app.data.CallLogHelper
import com.nefron.app.data.Slot
import com.nefron.app.data.SlotStorage
import java.util.Calendar
import kotlinx.coroutines.launch

private val DURATION_OPTIONS = listOf(15, 30)

private fun slotHeight(durationMinutes: Int) =
    if (durationMinutes <= 15) 52.dp else 72.dp

@Composable
fun ScheduleScreen() {
    val isSunday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    if (isSunday) {
        ClosedScreen()
        return
    }

    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedDay     by remember { mutableStateOf(todayLabel()) }
    var slots           by remember { mutableStateOf(SlotStorage.getSlotsForDay(context, selectedDay)) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun refresh() {
        slots = SlotStorage.getSlotsForDay(context, selectedDay)
    }

    Scaffold(
        modifier      = Modifier.statusBarsPadding(),
        snackbarHost  = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showResetDialog = true },
                containerColor = Color(0xFF6B9DC2)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset week", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            ScrollableTabRow(selectedTabIndex = SlotStorage.DAYS.indexOf(selectedDay)) {
                SlotStorage.DAYS.forEach { day ->
                    Tab(
                        selected = day == selectedDay,
                        onClick  = {
                            selectedDay = day
                            slots = SlotStorage.getSlotsForDay(context, day)
                        },
                        text = { Text(day, fontWeight = FontWeight.Bold) }
                    )
                }
            }
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(slots, key = { it.index }) { slot ->
                    val nextSlot = slots.getOrNull(slot.index + 1)
                    SlotRow(
                        slot     = slot,
                        nextSlot = nextSlot,
                        onTap       = {
                            if (slot.phone == null) {
                                SlotStorage.setPhone(context, selectedDay, slot.index, "")
                                refresh()
                            }
                        },
                        onPasteCall = {
                            val number = CallLogHelper.getLastIncomingCall(context) ?: return@SlotRow
                            val bookedNumbers = slots.mapNotNull { it.phone }.filter { it.isNotBlank() }
                            if (bookedNumbers.contains(number)) {
                                scope.launch { snackbar.showSnackbar("$number is already booked") }
                            } else {
                                SlotStorage.setPhone(context, selectedDay, slot.index, number)
                                refresh()
                            }
                        },
                        onUnbook    = {
                            SlotStorage.clearPhone(context, selectedDay, slot.index)
                            refresh()
                        },
                        onSetDuration = { minutes ->
                            SlotStorage.setDuration(context, selectedDay, slot.index, minutes)
                            refresh()
                        }
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlotRow(
    slot: Slot,
    nextSlot: Slot?,
    onTap: () -> Unit,
    onPasteCall: () -> Unit,
    onUnbook: () -> Unit,
    onSetDuration: (Int) -> Unit
) {
    val assigned = slot.phone != null
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(slotHeight(slot.durationMinutes))
                .combinedClickable(
                    onClick      = { if (!assigned) onTap() },
                    onLongClick  = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (assigned) MaterialTheme.colorScheme.primaryContainer
                                 else Color(0xFFF0F4F8)
            )
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(72.dp)) {
                    Text(
                        text       = slot.startTime,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (assigned) MaterialTheme.colorScheme.onPrimaryContainer
                                     else Color(0xFF4A6070)
                    )
                    Text(
                        text       = "${slot.durationMinutes}m",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (assigned) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                     else Color(0xFF8AA0B0)
                    )
                }
                Text(
                    text     = slot.phone?.ifBlank { "Booked" } ?: "available",
                    fontSize = 16.sp,
                    color    = if (assigned) MaterialTheme.colorScheme.onPrimaryContainer
                               else Color(0xFF8AA0B0),
                    modifier = Modifier.weight(1f)
                )
                if (assigned) {
                    IconButton(onClick = onPasteCall) {
                        Icon(
                            imageVector        = Icons.Default.PhoneCallback,
                            contentDescription = "Paste last call",
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onUnbook) {
                        Icon(
                            imageVector        = Icons.Default.Clear,
                            contentDescription = "Unbook",
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DURATION_OPTIONS.forEach { minutes ->
                val enabled = if (minutes <= slot.durationMinutes) {
                    true // shrink always allowed
                } else {
                    val delta = minutes - slot.durationMinutes
                    nextSlot != null && nextSlot.phone == null && delta <= nextSlot.durationMinutes
                }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$minutes min", modifier = Modifier.weight(1f))
                            if (minutes == slot.durationMinutes) {
                                Icon(
                                    imageVector        = Icons.Default.Check,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        showMenu = false
                        onSetDuration(minutes)
                    },
                    enabled = enabled
                )
            }
        }
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
            Text("Closed", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B9DC2))
            Text(
                text      = "The clinic is closed on Sundays.\nSee you tomorrow.",
                fontSize  = 16.sp,
                color     = Color(0xFF8AA0B0),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun todayLabel() = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY    -> "MON"
    Calendar.TUESDAY   -> "TUE"
    Calendar.WEDNESDAY -> "WED"
    Calendar.THURSDAY  -> "THU"
    Calendar.FRIDAY    -> "FRI"
    else               -> "SAT"
}
