package com.nickdegs.sosyalpanel.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nickdegs.sosyalpanel.data.AccountWithSnapshots
import com.nickdegs.sosyalpanel.data.FollowAnalysis
import com.nickdegs.sosyalpanel.data.FollowUser
import com.nickdegs.sosyalpanel.data.FollowerAnalysisService
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.theme.Brand
import com.nickdegs.sosyalpanel.ui.theme.BrandBlue
import com.nickdegs.sosyalpanel.ui.theme.Mint

// Takip analizi (Bluesky) — geri takip etmeyenler / takip etmediklerin.
@Composable
fun FollowerAnalysisDialog(acc: AccountWithSnapshots, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var analysis by remember { mutableStateOf<FollowAnalysis?>(null) }
    var tab by remember { mutableStateOf(0) } // 0=geri takip etmeyen, 1=takip etmediklerin

    LaunchedEffect(acc.account.id) {
        loading = true
        analysis = FollowerAnalysisService.analyze(acc.account.platform, acc.account.username)
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // Üst bar
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Takip Analizi", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Kapat") }
                }

                val a = analysis
                when {
                    loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Takip listesi analiz ediliyor…", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    a == null -> EmptyMsg("Analiz yapılamadı",
                        "Hesap gizli olabilir ya da liste herkese açık değil. Bluesky hesaplarında çalışır.")
                    else -> {
                        // Özet
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Stat("Takipçi", a.followerCount, Brand, Modifier.weight(1f))
                            Stat("Takip", a.followingCount, BrandBlue, Modifier.weight(1f))
                            Stat("Karşılıklı", a.mutualCount, Mint, Modifier.weight(1f))
                        }
                        // Sekme
                        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                            Tab(selected = tab == 0, onClick = { tab = 0 },
                                text = { Text("Geri takip etmeyen (${a.notFollowingBack.size})", fontSize = 12.sp) })
                            Tab(selected = tab == 1, onClick = { tab = 1 },
                                text = { Text("Takip etmediklerin (${a.youDontFollowBack.size})", fontSize = 12.sp) })
                        }
                        val list = if (tab == 0) a.notFollowingBack else a.youDontFollowBack
                        if (list.isEmpty()) {
                            EmptyMsg(if (tab == 0) "Herkes geri takip ediyor 🎉" else "Hepsini geri takip ediyorsun 👍", "")
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(list, key = { it.id }) { u -> UserRow(u) { openUrl(ctx, u.profileUrl) } }
                                if (a.truncated) item {
                                    Text("Çok büyük hesap — liste kısmen gösteriliyor.",
                                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun UserRow(u: FollowUser, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Brand.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(u.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Brand)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(u.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(u.handle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun EmptyMsg(title: String, sub: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (sub.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center)
            }
        }
    }
}

private fun openUrl(ctx: android.content.Context, url: String?) {
    if (url.isNullOrBlank()) return
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
