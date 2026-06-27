package com.nickdegs.sosyalpanel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nickdegs.sosyalpanel.data.AccountWithSnapshots
import com.nickdegs.sosyalpanel.data.FollowAnalysis
import com.nickdegs.sosyalpanel.data.FollowerAnalysisService

// Takip analizi (Bluesky canlı) — geri takip etmeyenler / takip etmediklerin.
@Composable
fun FollowerAnalysisDialog(acc: AccountWithSnapshots, onDismiss: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var analysis by remember { mutableStateOf<FollowAnalysis?>(null) }

    LaunchedEffect(acc.account.id) {
        loading = true
        analysis = FollowerAnalysisService.analyze(acc.account.platform, acc.account.username)
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
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
                    a == null -> AnalysisEmptyMsg("Analiz yapılamadı",
                        "Hesap gizli olabilir ya da liste herkese açık değil. Bluesky hesaplarında çalışır.")
                    else -> AnalysisResultContent(a)
                }
            }
        }
    }
}
