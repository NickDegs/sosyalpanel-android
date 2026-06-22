package com.nickdegs.sosyalpanel.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.data.AIService
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.theme.Brand
import kotlinx.coroutines.launch

private data class Tool(val id: String, val name: String, val kind: String, val hint: String, val extra: Map<String, Any> = emptyMap())

private val TOOLS = listOf(
    Tool("yazi", "Caption / Yazı", "Metin", "Ne hakkında yazı?"),
    Tool("icerik", "İçerik Fikri", "Metin", "Konu / niş", mapOf("platform" to "instagram")),
    Tool("gorsel", "Görsel Üret", "Görsel", "Görseli tarif et"),
    Tool("logo", "Logo", "Görsel", "Marka + stil"),
    Tool("video", "Video", "Video", "Video sahnesi tarif et"),
    Tool("seo", "SEO Metni", "Metin", "Ürün/sayfa konusu"),
    Tool("ceviri", "Çeviri", "Metin", "Çevrilecek metin", mapOf("hedef" to "İngilizce")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(vm: AppViewModel, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<Tool?>(null) }
    val sel = selected
    if (sel != null) {
        ToolRun(sel, vm, onBack = { selected = null }); return
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("AI Stüdyo", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(TOOLS) { t ->
                GlassCard(modifier = Modifier.clickable { selected = t }) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Brand, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(t.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(t.kind, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolRun(tool: Tool, vm: AppViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var textOut by remember { mutableStateOf<String?>(null) }
    var imageOut by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var videoOut by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPro by remember { mutableStateOf(false) }

    fun run() {
        val t = prompt.trim()
        if (t.isBlank() || loading) return
        loading = true; error = null; textOut = null; imageOut = null; videoOut = null
        val params = mutableMapOf<String, Any>("prompt" to t, "text" to t, "konu" to t)
        params.putAll(tool.extra)
        scope.launch {
            when (val r = AIService.runTool(tool.id, params)) {
                is AIService.ToolResult.TextOut -> textOut = r.text
                is AIService.ToolResult.ImageOut -> {
                    val b64 = r.dataUri.substringAfter(",", r.dataUri)
                    runCatching {
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        imageOut = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.onFailure { error = "Görsel okunamadı" }
                }
                is AIService.ToolResult.VideoOut -> videoOut = r.url
                AIService.ToolResult.Quota -> showPro = true
                AIService.ToolResult.Error -> error = "Üretim başarısız oldu, tekrar dene."
            }
            loading = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(tool.name, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(value = prompt, onValueChange = { prompt = it },
                placeholder = { Text(tool.hint) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
            Button(onClick = { run() }, enabled = prompt.isNotBlank() && !loading, modifier = Modifier.fillMaxWidth()) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                else { Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Üret") }
            }
            error?.let { Text(it, color = com.nickdegs.sosyalpanel.ui.theme.Danger, fontSize = 13.sp) }
            textOut?.let { SelectionContainer { GlassCard { Text(it, fontSize = 14.sp) } } }
            imageOut?.let {
                Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)))
            }
            videoOut?.let { Text("Video hazır: $it", fontSize = 13.sp, color = Brand) }
        }
    }
    if (showPro) ProDialog(vm) { showPro = false }
}
