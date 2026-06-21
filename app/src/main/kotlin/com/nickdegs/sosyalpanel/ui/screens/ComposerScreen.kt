package com.nickdegs.sosyalpanel.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.R
import com.nickdegs.sosyalpanel.data.AppOpener
import com.nickdegs.sosyalpanel.data.Platform
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.components.PlatformBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    var showPlanner by remember { mutableStateOf(false) }

    if (showPlanner) {
        PlannerScreen(vm, onBack = { showPlanner = false })
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_share), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { showPlanner = true }) {
                        Icon(Icons.Filled.CalendarMonth, stringResource(R.string.content_calendar))
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text(stringResource(R.string.what_share), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Platform.entries.toList()) { p ->
                    GlassCard(modifier = Modifier.clickable { AppOpener.open(ctx, p) }) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            PlatformBadge(p, 44)
                            Spacer(Modifier.height(8.dp))
                            Text(p.displayName, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
