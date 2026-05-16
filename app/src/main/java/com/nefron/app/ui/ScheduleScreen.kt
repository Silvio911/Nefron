package com.nefron.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nefron.app.data.CallEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nefron.app.data.CallLogHelper
import com.nefron.app.data.Slot
import com.nefron.app.data.SlotStorage
import java.util.Calendar

private val DURATION_OPTIONS = listOf(15, 30)

private fun slotHeight(durationMinutes: Int) =
    if (durationMinutes <= 15) 52.dp else 72.dp

@Composable
fun ScheduleScreen() {
    val context = LocalContext.current
    var selectedDay     by remember { mutableStateOf(todayLabel()) }
    var slots           by remember { mutableStateOf(SlotStorage.getSlotsForDay(context, selectedDay)) }
    var showResetDialog by remember { mutableStateOf(false) }
    var callPickerSlot  by remember { mutableStateOf<Slot?>(null) }
    var pasteDialog     by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var pendingPickerSlot by remember { mutableStateOf<Slot?>(null) }
    var recentCalls       by remember { mutableStateOf<List<CallEntry>>(emptyList()) }

    LaunchedEffect(callPickerSlot) {
        recentCalls = if (callPickerSlot != null) {
            withContext(Dispatchers.IO) { CallLogHelper.getRecentIncomingCalls(context) }
        } else emptyList()
    }

    val requestContacts = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // open the sheet regardless — names show if granted, numbers if not
        callPickerSlot = pendingPickerSlot
        pendingPickerSlot = null
    }

    fun refresh() {
        slots = SlotStorage.getSlotsForDay(context, selectedDay)
    }

    Scaffold(
        modifier             = Modifier.statusBarsPadding(),
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
            PrimaryScrollableTabRow(selectedTabIndex = SlotStorage.DAYS.indexOf(selectedDay)) {
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
                modifier        = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding  = PaddingValues(top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(slots, key = { it.index }) { slot ->
                    val nextSlot = slots.getOrNull(slot.index + 1)
                    SlotRow(
                        slot     = slot,
                        nextSlot = nextSlot,
                        onTap       = {
                            SlotStorage.setPhone(context, selectedDay, slot.index, "")
                            refresh()
                        },
                        onPasteCall = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                                == PackageManager.PERMISSION_GRANTED) {
                                callPickerSlot = slot
                            } else {
                                pendingPickerSlot = slot
                                requestContacts.launch(Manifest.permission.READ_CONTACTS)
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

    callPickerSlot?.let { slot ->
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { callPickerSlot = null },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Text(
                text     = "Recent calls",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            if (recentCalls.isEmpty()) {
                Text(
                    text     = "No recent incoming calls found.",
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    recentCalls.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    callPickerSlot = null
                                    pasteDialog = Pair(slot.index, entry.display)
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = entry.time,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(60.dp)
                            )
                            Column {
                                Text(
                                    text       = entry.display,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 16.sp
                                )
                                if (entry.name != null) {
                                    Text(
                                        text  = entry.number,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    pasteDialog?.let { (slotIndex, initialText) ->
        var text by remember(slotIndex) { mutableStateOf(initialText) }
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { pasteDialog = null },
            title = { Text(slots.find { it.index == slotIndex }?.startTime ?: "") },
            text  = {
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    label         = { Text("Name or note") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    SlotStorage.setPhone(context, selectedDay, slotIndex, text)
                    pasteDialog = null
                    refresh()
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { pasteDialog = null }) { Text("Cancel") }
            }
        )
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
                            imageVector        = Icons.Default.PersonAdd,
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


private fun todayLabel() = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY    -> "MON"
    Calendar.TUESDAY   -> "TUE"
    Calendar.WEDNESDAY -> "WED"
    Calendar.THURSDAY  -> "THU"
    Calendar.FRIDAY    -> "FRI"
    Calendar.SATURDAY  -> "SAT"
    else               -> "MON" // Sunday — show next week's Monday
}
