package com.nickdegs.sosyalpanel.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Public metrik — SADECE auth gerektirmeyen RESMİ public API'ler.
// Scraping YOK, kullanıcı şifresi YOK → Play tamamen güvenli.
data class PublicMetrics(val followers: Int, val following: Int?, val posts: Int?)

object PublicMetricsService {

    // YouTube Data API anahtarı (varsa). Yoksa YouTube otomatik çekme devre dışı.
    private val youtubeApiKey: String? = null

    fun isSupported(p: Platform): Boolean = when (p) {
        Platform.BLUESKY -> true
        Platform.YOUTUBE -> youtubeApiKey != null
        else -> false
    }

    suspend fun fetch(platform: Platform, rawUser: String): PublicMetrics? = withContext(Dispatchers.IO) {
        val user = rawUser.trim().removePrefix("@")
        when (platform) {
            Platform.BLUESKY -> bluesky(user)
            Platform.YOUTUBE -> youtube(user)
            else -> null
        }
    }

    // Bluesky (AT Protocol public — auth yok)
    private fun bluesky(handle: String): PublicMetrics? {
        val actor = if (handle.contains(".")) handle else "$handle.bsky.social"
        val json = getJson("https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile?actor=$actor") ?: return null
        if (!json.has("followersCount")) return null
        return PublicMetrics(
            followers = json.optInt("followersCount"),
            following = json.optInt("followsCount").takeIf { json.has("followsCount") },
            posts = json.optInt("postsCount").takeIf { json.has("postsCount") }
        )
    }

    // YouTube (Data API v3 — API anahtarı gerekir, public veri)
    private fun youtube(handle: String): PublicMetrics? {
        val key = youtubeApiKey ?: return null
        val h = if (handle.startsWith("@")) handle else "@$handle"
        val json = getJson("https://www.googleapis.com/youtube/v3/channels?part=statistics&forHandle=$h&key=$key") ?: return null
        val items = json.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        val stats = items.getJSONObject(0).optJSONObject("statistics") ?: return null
        return PublicMetrics(
            followers = stats.optString("subscriberCount").toIntOrNull() ?: 0,
            following = null,
            posts = stats.optString("videoCount").toIntOrNull()
        )
    }

    private fun getJson(url: String): JSONObject? {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000; readTimeout = 15000
                setRequestProperty("User-Agent", "SosyalPanel/1.0")
            }
            if (conn.responseCode != 200) { conn.disconnect(); return null }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            JSONObject(body)
        } catch (_: Exception) { null }
    }
}
