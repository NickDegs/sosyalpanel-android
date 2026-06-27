package com.nickdegs.sosyalpanel.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nickdegs.sosyalpanel.data.AccountWithSnapshots
import com.nickdegs.sosyalpanel.data.DataImportService
import com.nickdegs.sosyalpanel.data.FollowAnalysis
import com.nickdegs.sosyalpanel.data.Platform
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.theme.Mint
import kotlinx.coroutines.launch

// Resmi veri içe aktarma — Instagram/TikTok "Verilerini İndir" dosyalarından
// takip analizi (geri takip etmeyen). Tüm işlem cihazda; ağ/kazıma/şifre yok.
@Composable
fun DataImportDialog(acc: AccountWithSnapshots, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val isTikTok = acc.account.platform == Platform.TIKTOK
    var followersUri by remember { mutableStateOf<Uri?>(null) }
    var followingUri by remember { mutableStateOf<Uri?>(null) }
    var combinedUri by remember { mutableStateOf<Uri?>(null) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var analysis by remember { mutableStateOf<FollowAnalysis?>(null) }

    val anyType = arrayOf("*/*")
    val pickFollowers = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { followersUri = it; error = null } }
    val pickFollowing = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { followingUri = it; error = null } }
    val pickCombined = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { combinedUri = it; error = null } }

    val ready = if (isTikTok) combinedUri != null else (followersUri != null && followingUri != null)

    fun run() {
        working = true; error = null
        scope.launch {
            val result: FollowAnalysis? = if (isTikTok) {
                combinedUri?.let { DataImportService.tiktok(ctx, it) }
            } else {
                val f = followersUri; val g = followingUri
                if (f != null && g != null) DataImportService.generic(ctx, f, g, acc.account.platform) else null
            }
            working = false
            if (result != null) analysis = result
            else error = "Dosya okunamadı. JSON formatında ve doğru dosyaları seçtiğinden emin ol."
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (analysis == null) "Veri İçe Aktar" else "Takip Analizi",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (analysis != null) {
                        TextButton(onClick = { analysis = null }) { Text("Yeniden") }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Kapat") }
                }

                val a = analysis
                if (a != null) {
                    AnalysisResultContent(a)
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Rehber
                        GlassCard {
                            Text("Veri dosyasını nasıl alırım?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            steps(isTikTok).forEach {
                                Text("•  $it", fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 1.dp))
                            }
                        }

                        if (isTikTok) {
                            FileRow("TikTok veri dosyası (user_data.json)", combinedUri?.lastPathSegment) {
                                pickCombined.launch(anyType)
                            }
                        } else {
                            FileRow("Takipçiler (followers_1.json)", followersUri?.lastPathSegment) {
                                pickFollowers.launch(anyType)
                            }
                            FileRow("Takip edilenler (following.json)", followingUri?.lastPathSegment) {
                                pickFollowing.launch(anyType)
                            }
                        }

                        error?.let {
                            Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        }

                        Button(
                            onClick = { run() },
                            enabled = ready && !working,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (working) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (working) "İşleniyor…" else "Analiz Et", fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, null, tint = Mint, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Dosyalar yalnızca telefonunda işlenir. Hiçbir yere yüklenmez, şifre istenmez.",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(title: String, picked: String?, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (picked == null) Icons.Filled.UploadFile else Icons.Filled.CheckCircle,
                null,
                tint = if (picked == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Mint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(picked ?: "Dosya seç", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
            }
            TextButton(onClick = onClick) { Text("Seç") }
        }
    }
}

private fun steps(isTikTok: Boolean): List<String> = if (isTikTok) listOf(
    "TikTok → Ayarlar → Hesap → Verilerini indir",
    "Format JSON seç, talebi gönder",
    "Hazır olunca indir, ZIP'i dosya yöneticisinde aç",
    "user_data.json dosyasını buradan seç"
) else listOf(
    "Instagram → Ayarlar → Hesap Merkezi → Bilgilerini indir",
    "Format JSON, içerik: Takipçiler ve takip edilenler",
    "Hazır olunca indir, ZIP'i dosya yöneticisinde aç",
    "followers_1.json ve following.json dosyalarını seç"
)
