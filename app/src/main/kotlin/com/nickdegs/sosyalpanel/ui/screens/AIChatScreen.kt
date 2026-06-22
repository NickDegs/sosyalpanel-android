package com.nickdegs.sosyalpanel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.data.AIService
import com.nickdegs.sosyalpanel.data.AccountWithSnapshots
import com.nickdegs.sosyalpanel.data.PublicMetrics
import com.nickdegs.sosyalpanel.data.PublicMetricsService
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.components.PlatformBadge
import com.nickdegs.sosyalpanel.ui.theme.Brand
import com.nickdegs.sosyalpanel.ui.theme.Gold
import kotlinx.coroutines.launch

private data class ChatMsg(val role: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(vm: AppViewModel = viewModel()) {
    val accounts by vm.accounts.collectAsState()
    val isPro by vm.billing.isPro.collectAsState()
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf<ChatMsg>()) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var smartMode by remember { mutableStateOf(false) }
    var freeLeft by remember { mutableStateOf<Int?>(null) }
    var showPro by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { freeLeft = AIService.freeRemaining() }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size) }

    fun ask(realPrompt: String, display: String = realPrompt) {
        val t = realPrompt.trim()
        if (t.isEmpty() || loading) return
        val usePro = smartMode && isPro
        messages = messages + ChatMsg("user", display)
        val apiHistory = messages.toMutableList().also { it[it.size - 1] = ChatMsg("user", t) }
        input = ""; loading = true
        scope.launch {
            val res = AIService.chat(apiHistory.map { it.role to it.text }, usePro)
            when {
                res.quota -> showPro = true
                res.text != null -> {
                    messages = messages + ChatMsg("assistant", res.text!!)
                    if (!isPro) freeLeft = AIService.freeRemaining()
                }
                else -> messages = messages + ChatMsg("assistant", "Bağlantı sorunu oldu, tekrar dener misin?")
            }
            loading = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("AI Asistan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    if (isPro) {
                        Icon(Icons.Filled.AutoAwesome, "Pro", tint = Gold, modifier = Modifier.padding(end = 12.dp))
                    } else freeLeft?.let {
                        Text("$it ücretsiz", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 12.dp))
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
                if (!isPro) {
                    FilterChip(selected = smartMode, onClick = {
                        if (!isPro) showPro = true else smartMode = !smartMode
                    }, label = { Text("Daha Akıllı Yanıt (Pro)", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp), tint = Gold) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        placeholder = { Text("Bir şey sor…") },
                        modifier = Modifier.weight(1f), maxLines = 4
                    )
                    IconButton(onClick = { ask(input) }, enabled = input.isNotBlank() && !loading) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Gönder",
                            tint = if (input.isNotBlank()) Brand else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (messages.isEmpty()) {
                Starter(accounts, onAnalyze = { acc ->
                    scope.launch {
                        val m = PublicMetricsService.fetch(acc.account.platform, acc.account.username)
                            ?: acc.latest?.let { PublicMetrics(it.followers, it.following, it.posts) }
                        ask(AIService.analysisPrompt(acc.account.platform.displayName, acc.account.username, m),
                            "@${acc.account.username} hesabımı analiz et")
                    }
                }, onQuick = { ask(it) })
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(messages) { Bubble(it) }
                    if (loading) item {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp)); Text("yazıyor…", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }

    if (showPro) ProDialog(vm) { showPro = false; smartMode = false }
}

@Composable
private fun Starter(accounts: List<AccountWithSnapshots>, onAnalyze: (AccountWithSnapshots) -> Unit, onQuick: (String) -> Unit) {
    val quick = listOf(
        "Bu hafta için 5 viral içerik fikri ver",
        "Etkileşimimi artırmak için ne yapmalıyım?",
        "Reels/Shorts için kancalı 3 başlık öner",
        "Profilimi optimize etmek için ipuçları ver"
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Brand, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(8.dp))
                Text("Sosyal Medya AI Asistanın", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Hesaplarını analiz edeyim, büyüme stratejisi ve içerik fikirleri vereyim.",
                    fontSize = 13.sp, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
        if (accounts.isNotEmpty()) {
            item { Label("Hesaplarını Analiz Et") }
            items(accounts) { acc ->
                GlassCard(modifier = Modifier.clickable { onAnalyze(acc) }, tint = acc.account.platform.brandColor) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlatformBadge(acc.account.platform, 34)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("@${acc.account.username}", fontWeight = FontWeight.Medium)
                            Text("${acc.account.platform.displayName} · AI analizi", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Icon(Icons.Filled.AutoAwesome, null, tint = Brand, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        item { Label("Hızlı Başla") }
        items(quick) { p ->
            GlassCard(modifier = Modifier.clickable { onQuick(p) }) {
                Text(p, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp, top = 6.dp))
}

@Composable
private fun Bubble(msg: ChatMsg) {
    val isUser = msg.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier.widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isUser) Brand.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(msg.text, fontSize = 14.sp, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}
