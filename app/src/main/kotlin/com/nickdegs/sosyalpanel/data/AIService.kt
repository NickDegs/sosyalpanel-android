package com.nickdegs.sosyalpanel.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL

// RealVirtuality AI (ai.nickdegs.com) — Cloudflare Workers AI.
// Ücretsiz sohbet /api/sohbet (IP başına günlük kota), akıllı sohbet /api/pro (Pro).
// Kota dolunca 402 → uygulama Pro paywall gösterir.

data class AiResult(val text: String?, val quota: Boolean = false, val error: Boolean = false)

object AIService {
    private const val BASE = "https://ai.nickdegs.com"

    // Çerez (uid) yönetimi — oturum içi tutulur.
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)

    suspend fun freeRemaining(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val json = getJson("$BASE/api/durum") ?: return@runCatching null
            if (json.has("free_kalan")) json.getInt("free_kalan") else null
        }.getOrNull()
    }

    suspend fun chat(history: List<Pair<String, String>>, pro: Boolean): AiResult = withContext(Dispatchers.IO) {
        val path = if (pro) "/api/pro" else "/api/sohbet"
        val body = JSONObject()
        if (pro) {
            body.put("istek", history.lastOrNull()?.second ?: "")
        } else {
            val arr = JSONArray()
            history.forEach { (role, content) ->
                arr.put(JSONObject().put("role", if (role == "assistant") "assistant" else "user").put("content", content))
            }
            body.put("messages", arr)
        }
        postJson(path, body)
    }

    // Premium araç çıktısı
    sealed class ToolResult {
        data class TextOut(val text: String) : ToolResult()
        data class ImageOut(val dataUri: String) : ToolResult()
        data class VideoOut(val url: String) : ToolResult()
        object Quota : ToolResult()
        object Error : ToolResult()
    }

    suspend fun runTool(endpoint: String, params: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
        params.forEach { (k, v) -> body.put(k, v) }
        val conn = open("$BASE/api/$endpoint")
        try {
            conn.requestMethod = "POST"; conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            storeCookies(conn)
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()
            when {
                code == 402 || json?.optString("err") == "kota_doldu" -> ToolResult.Quota
                json?.has("image") == true -> ToolResult.ImageOut(json.getString("image"))
                json?.has("video") == true -> ToolResult.VideoOut(json.getString("video"))
                json?.optBoolean("ok") == true && json.optString("metin").isNotBlank() ->
                    ToolResult.TextOut(json.getString("metin"))
                else -> ToolResult.Error
            }
        } catch (_: Exception) { ToolResult.Error } finally { conn.disconnect() }
    }

    // role: "user" | "assistant"
    fun analysisPrompt(platform: String, username: String, metrics: PublicMetrics?): String {
        val sb = StringBuilder("Sen üst düzey bir sosyal medya büyüme stratejistisin. Kısa, uygulanabilir, profesyonel tavsiye ver. ")
        sb.append("Platform: $platform, kullanıcı: @$username. ")
        if (metrics != null) {
            sb.append("Güncel veriler — takipçi: ${metrics.followers}")
            metrics.following?.let { sb.append(", takip: $it") }
            metrics.posts?.let { sb.append(", gönderi: $it") }
            sb.append(". Bu hesabı analiz et: güçlü/zayıf yönler, büyüme için 3 somut aksiyon, içerik önerisi.")
        } else {
            sb.append("Bu hesap için büyüme stratejisi, içerik fikirleri ve etkileşim taktikleri öner.")
        }
        return sb.toString()
    }

    // --- HTTP ---
    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000; conn.readTimeout = 45000
        conn.setRequestProperty("User-Agent", "SosyalPanel/1.0")
        // çerezleri ekle
        cookies.cookieStore.cookies.takeIf { it.isNotEmpty() }?.let {
            conn.setRequestProperty("Cookie", it.joinToString("; ") { c -> "${c.name}=${c.value}" })
        }
        return conn
    }

    private fun storeCookies(conn: HttpURLConnection) {
        conn.headerFields["Set-Cookie"]?.forEach { sc ->
            runCatching { cookies.put(conn.url.toURI(), mapOf("Set-Cookie" to listOf(sc))) }
        }
    }

    private fun getJson(url: String): JSONObject? {
        val conn = open(url)
        return try {
            if (conn.responseCode != 200) return null
            storeCookies(conn)
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (_: Exception) { null } finally { conn.disconnect() }
    }

    private fun postJson(path: String, body: JSONObject): AiResult {
        val conn = open("$BASE$path")
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            storeCookies(conn)
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()
            when {
                code == 402 || json?.optString("err") == "kota_doldu" -> AiResult(null, quota = true)
                json?.optBoolean("ok") == true -> AiResult(json.optString("metin").ifBlank { null })
                else -> AiResult(null, error = true)
            }
        } catch (_: Exception) { AiResult(null, error = true) } finally { conn.disconnect() }
    }
}
