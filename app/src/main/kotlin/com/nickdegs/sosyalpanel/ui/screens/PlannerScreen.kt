package com.nickdegs.sosyalpanel.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.R
import com.nickdegs.sosyalpanel.data.Platform
import com.nickdegs.sosyalpanel.data.ScheduledPost
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.components.PlatformBadge
import com.nickdegs.sosyalpanel.ui.theme.Danger
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(vm: AppViewModel, onBack: () -> Unit) {
    val posts by vm.scheduledPosts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val upcoming = posts.filter { it.scheduledAt >= now }
    val past = posts.filter { it.scheduledAt < now }.reversed()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.content_calendar), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.CalendarMonth, stringResource(R.string.plan_post))
            }
        }
    ) { pad ->
        if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                GlassCard(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.no_plans), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.no_plans_desc), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (upcoming.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.upcoming)) }
                    items(upcoming, key = { it.id }) { PlanRow(it) { vm.deleteScheduledPost(it) } }
                }
                if (past.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.past)) }
                    items(past, key = { it.id }) { PlanRow(it) { vm.deleteScheduledPost(it) } }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) AddPlanDialog(onDismiss = { showAdd = false }) { note, platform, atMillis, notify ->
        vm.addScheduledPost(note, platform, atMillis, notify); showAdd = false
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

@Composable
private fun PlanRow(post: ScheduledPost, onDelete: () -> Unit) {
    GlassCard(tint = post.platform.brandColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformBadge(post.platform, 38)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(post.note.ifBlank { post.platform.displayName }, fontWeight = FontWeight.Medium, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(post.scheduledAt)),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    if (post.notify && !post.isPast) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null, tint = Danger) }
        }
    }
}

@Composable
private fun AddPlanDialog(
    onDismiss: () -> Unit,
    onSave: (String, Platform, Long, Boolean) -> Unit
) {
    val ctx = LocalContext.current
    var note by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf(Platform.INSTAGRAM) }
    var expanded by remember { mutableStateOf(false) }
    var notify by remember { mutableStateOf(true) }
    val cal = remember { Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) } }
    var whenMillis by remember { mutableLongStateOf(cal.timeInMillis) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun pickDateTime() {
        val c = Calendar.getInstance().apply { timeInMillis = whenMillis }
        DatePickerDialog(ctx, { _, y, m, d ->
            TimePickerDialog(ctx, { _, h, min ->
                c.set(y, m, d, h, min, 0)
                whenMillis = c.timeInMillis
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_post)) },
        text = {
            Column {
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text(stringResource(R.string.content_note)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                Spacer(Modifier.height(10.dp))
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        PlatformBadge(platform, 22); Spacer(Modifier.width(8.dp)); Text(platform.displayName)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        Platform.entries.forEach { p ->
                            DropdownMenuItem(text = { Text(p.displayName) }, onClick = { platform = p; expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { pickDateTime() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(whenMillis)))
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notify, onCheckedChange = {
                        notify = it
                        if (it && Build.VERSION.SDK_INT >= 33) {
                            permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    })
                    Text(stringResource(R.string.reminder_notification), fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (whenMillis > System.currentTimeMillis()) onSave(note.trim(), platform, whenMillis, notify)
            }) { Text(stringResource(R.string.plan_action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
