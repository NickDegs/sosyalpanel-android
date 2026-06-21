package com.nickdegs.sosyalpanel.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.R
import com.nickdegs.sosyalpanel.data.AccountWithSnapshots
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.components.PlatformBadge
import com.nickdegs.sosyalpanel.ui.theme.Danger
import com.nickdegs.sosyalpanel.ui.theme.Mint
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsState()
    val ctx = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_analytics), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { exportCsv(ctx, accounts) }) {
                        Icon(Icons.Filled.IosShare, stringResource(R.string.export_csv))
                    }
                }
            )
        }
    ) { pad ->
        if (accounts.all { it.snapshots.size < 2 }) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                GlassCard(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.not_enough_data), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.enter_two_points), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts.filter { it.snapshots.size >= 2 }, key = { it.account.id }) { acc ->
                AnalyticsCard(acc)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AnalyticsCard(acc: AccountWithSnapshots) {
    val growth = acc.growthPercent()
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformBadge(acc.account.platform, 32)
            Spacer(Modifier.width(10.dp))
            Text("@${acc.account.username}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                (if (growth >= 0) "+" else "") + "%.1f%%".format(growth),
                color = if (growth >= 0) Mint else Danger, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        Sparkline(acc.sorted.map { it.followers })
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat(stringResource(R.string.daily_avg), "${if (acc.dailyAverage() >= 0) "+" else ""}${acc.dailyAverage()}")
            Stat(stringResource(R.string.followers), "${acc.latest?.followers ?: 0}")
            Stat(stringResource(R.string.posts), "${acc.latest?.posts ?: 0}")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun Sparkline(values: List<Int>) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
        if (values.size < 2) return@Canvas
        val min = values.min().toFloat(); val max = values.max().toFloat()
        val range = (max - min).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v ->
            Offset(i * stepX, size.height - ((v - min) / range) * size.height)
        }
        for (i in 0 until pts.size - 1) {
            drawLine(primary, pts[i], pts[i + 1], strokeWidth = 4f)
        }
        pts.forEach { drawCircle(primary, radius = 5f, center = it) }
    }
}

private fun exportCsv(ctx: android.content.Context, accounts: List<AccountWithSnapshots>) {
    val sb = StringBuilder("Platform,Username,Date,Followers,Following,Posts\n")
    accounts.forEach { acc ->
        acc.sorted.forEach { s ->
            sb.append("${acc.account.platform.displayName},@${acc.account.username},")
                .append("${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(s.capturedAt))},")
                .append("${s.followers},${s.following ?: ""},${s.posts ?: ""}\n")
        }
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(Intent.createChooser(send, "CSV").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
