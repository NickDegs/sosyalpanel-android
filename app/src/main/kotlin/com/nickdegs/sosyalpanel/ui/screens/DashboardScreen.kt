package com.nickdegs.sosyalpanel.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.R
import com.nickdegs.sosyalpanel.data.AccountWithSnapshots
import com.nickdegs.sosyalpanel.data.Milestone
import com.nickdegs.sosyalpanel.data.Platform
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.components.PlatformBadge
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsState()
    val isPro by vm.billing.isPro.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var updateFor by remember { mutableStateOf<AccountWithSnapshots?>(null) }
    var goalFor by remember { mutableStateOf<AccountWithSnapshots?>(null) }
    var showPro by remember { mutableStateOf(false) }

    val totalReach = accounts.sumOf { it.latest?.followers ?: 0 }

    // Açılışta desteklenen platformların public verisini resmi API'den tazele.
    LaunchedEffect(Unit) { vm.refreshSupported() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Social Panel", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                actions = {
                    IconButton(onClick = { vm.refreshSupported() }) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.update))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (vm.canAddAccount()) showAdd = true else showPro = true
            }) { Icon(Icons.Filled.Add, stringResource(R.string.add_account)) }
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(tint = MaterialTheme.colorScheme.primary) {
                    Text(stringResource(R.string.total_reach), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(4.dp))
                    Text(NumberFormat.getInstance().format(totalReach), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("${accounts.size} ${stringResource(R.string.accounts_suffix)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            if (accounts.isEmpty()) {
                item {
                    GlassCard {
                        Text(stringResource(R.string.no_accounts), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.tap_plus), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            } else {
                items(accounts, key = { it.account.id }) { acc ->
                    AccountCard(acc, onClick = { updateFor = acc }, onSetGoal = { goalFor = acc })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) AddAccountDialog(onDismiss = { showAdd = false }) { p, u ->
        vm.addAccount(p, u); showAdd = false
    }
    updateFor?.let { acc ->
        UpdateMetricDialog(acc, onDismiss = { updateFor = null }) { f, fl, p ->
            vm.addSnapshot(acc.account.id, f, fl, p); updateFor = null
        }
    }
    if (showPro) ProDialog(vm) { showPro = false }
    goalFor?.let { acc ->
        SetGoalDialog(acc, onDismiss = { goalFor = null }) { goal ->
            vm.setGoal(acc.account.id, goal); goalFor = null
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountCard(acc: AccountWithSnapshots, onClick: () -> Unit, onSetGoal: () -> Unit) {
    GlassCard(modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onSetGoal)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformBadge(acc.account.platform, 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("@${acc.account.username}", fontWeight = FontWeight.SemiBold)
                Text(acc.account.platform.displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(NumberFormat.getInstance().format(acc.currentFollowers), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(stringResource(R.string.followers_lower), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        // Hedef / milestone ilerleme şeridi
        val goal = acc.effectiveGoal
        if (acc.currentFollowers > 0 && goal != null) {
            Spacer(Modifier.height(10.dp))
            val brand = acc.account.platform.brandColor
            LinearProgressIndicator(
                progress = { acc.goalProgress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = brand,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Flag, null, tint = brand, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(Milestone.label(goal), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = brand)
                acc.goalEtaText?.let {
                    Text(" · $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Spacer(Modifier.weight(1f))
                Text("%${(acc.goalProgress * 100).toInt()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = brand)
            }
        }
    }
}

@Composable
fun SetGoalDialog(acc: AccountWithSnapshots, onDismiss: () -> Unit, onSave: (Int?) -> Unit) {
    var goalText by remember { mutableStateOf(acc.account.goalFollowers?.toString() ?: "") }
    val presets = listOf(1_000, 10_000, 50_000, 100_000, 1_000_000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(R.string.goal_title)} · @${acc.account.username}") },
        text = {
            Column {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.goal_followers)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { p ->
                        AssistChip(onClick = { goalText = p.toString() }, label = { Text(Milestone.label(p), fontSize = 12.sp) })
                    }
                }
                if (acc.reachedMilestones.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.reached_milestones), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        acc.reachedMilestones.takeLast(5).forEach { m ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, null, tint = com.nickdegs.sosyalpanel.ui.theme.Gold, modifier = Modifier.size(13.dp))
                                Text(Milestone.label(m), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = com.nickdegs.sosyalpanel.ui.theme.Gold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(goalText.toIntOrNull()) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onAdd: (Platform, String) -> Unit) {
    var platform by remember { mutableStateOf(Platform.INSTAGRAM) }
    var username by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_account)) },
        text = {
            Column {
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        PlatformBadge(platform, 24); Spacer(Modifier.width(8.dp)); Text(platform.displayName)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        Platform.entries.forEach { p ->
                            DropdownMenuItem(text = { Text(p.displayName) }, onClick = { platform = p; expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val supported = com.nickdegs.sosyalpanel.data.PublicMetricsService.isSupported(platform)
                Text(
                    if (supported) stringResource(R.string.auto_fetch_on)
                    else stringResource(R.string.auto_fetch_manual),
                    fontSize = 12.sp,
                    color = if (supported) com.nickdegs.sosyalpanel.ui.theme.Mint
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (username.isNotBlank()) onAdd(platform, username) }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun UpdateMetricDialog(acc: AccountWithSnapshots, onDismiss: () -> Unit, onSave: (Int, Int?, Int?) -> Unit) {
    var followers by remember { mutableStateOf(acc.latest?.followers?.toString() ?: "") }
    var following by remember { mutableStateOf(acc.latest?.following?.toString() ?: "") }
    var posts by remember { mutableStateOf(acc.latest?.posts?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("@${acc.account.username}") },
        text = {
            Column {
                num(followers, stringResource(R.string.followers)) { followers = it }
                Spacer(Modifier.height(8.dp))
                num(following, stringResource(R.string.following)) { following = it }
                Spacer(Modifier.height(8.dp))
                num(posts, stringResource(R.string.posts)) { posts = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val f = followers.toIntOrNull() ?: return@TextButton
                onSave(f, following.toIntOrNull(), posts.toIntOrNull())
            }) { Text(stringResource(R.string.update)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun num(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
