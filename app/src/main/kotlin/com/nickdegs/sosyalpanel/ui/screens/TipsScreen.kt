package com.nickdegs.sosyalpanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.R
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.theme.Gold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen() {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_tips), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->
        val tips = listOf(
            R.string.tip_questions, R.string.tip_lists, R.string.tip_quick,
            R.string.tip_throwback, R.string.tip_hashtag_count, R.string.tip_algo_hashtags,
            R.string.tip_specific_hashtags
        )
        val times = listOf(R.string.time_weekday_morning, R.string.time_weekday_noon, R.string.time_evening)

        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle(stringResource(R.string.content_ideas)) }
            items(tips) { t ->
                GlassCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Lightbulb, null, tint = Gold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(t), fontSize = 14.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)); SectionTitle(stringResource(R.string.best_times)) }
            items(times) { t ->
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(t), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}
