package com.nickdegs.sosyalpanel.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Takip analizi — "seni geri takip etmeyenler" + "senin geri takip etmediklerin".
// Bluesky (AT Protocol) resmi public API: getFollowers / getFollows — auth yok, scraping yok.
// Instagram/TikTok takipçi listesini public API'de vermediği için desteklenmez.

data class FollowUser(
    val id: String, val handle: String, val displayName: String,
    val avatar: String?, val profileUrl: String?
)

data class FollowAnalysis(
    val notFollowingBack: List<FollowUser>,   // takip ettiğin ama seni geri takip etmeyenler
    val youDontFollowBack: List<FollowUser>,  // seni takip eden ama senin etmediklerin
    val mutualCount: Int, val followerCount: Int, val followingCount: Int,
    val truncated: Boolean
)

object FollowerAnalysisService {

    private const val MAX_PAGES = 60   // ~6000 kişi güvenlik üst sınırı

    fun isSupported(p: Platform): Boolean = p == Platform.BLUESKY

    suspend fun analyze(platform: Platform, rawUser: String): FollowAnalysis? = withContext(Dispatchers.IO) {
        val user = rawUser.trim().removePrefix("@")
        when (platform) {
            Platform.BLUESKY -> bluesky(user)
            else -> null
        }
    }

    private fun bluesky(handle: String): FollowAnalysis? {
        val actor = if (handle.contains(".")) handle else "$handle.bsky.social"
        val (followers, fT) = bskyList("getFollowers", actor, "followers")
        val (follows, foT) = bskyList("getFollows", actor, "follows")
        if (followers == null && follows == null) return null
        val fr = followers ?: emptyList()
        val fo = follows ?: emptyList()
        val followerIds = fr.map { it.id }.toHashSet()
        val followingIds = fo.map { it.id }.toHashSet()
        return FollowAnalysis(
            notFollowingBack = fo.filter { it.id !in followerIds },
            youDontFollowBack = fr.filter { it.id !in followingIds },
            mutualCount = followerIds.count { it in followingIds },
            followerCount = fr.size,
            followingCount = fo.size,
            truncated = fT || foT
        )
    }

    // Bir Bluesky listesini sayfalayarak çeker → (kullanıcılar, kısaldıMı)
    private fun bskyList(endpoint: String, actor: String, key: String): Pair<List<FollowUser>?, Boolean> {
        val out = ArrayList<FollowUser>()
        var cursor: String? = null
        var pages = 0
        val enc = URLEncoder.encode(actor, "UTF-8")
        do {
            var url = "https://public.api.bsky.app/xrpc/app.bsky.graph.$endpoint?actor=$enc&limit=100"
            cursor?.let { url += "&cursor=" + URLEncoder.encode(it, "UTF-8") }
            val json = getJson(url) ?: return if (out.isEmpty()) null to false else out to true
            val arr = json.optJSONArray(key)
            if (arr != null) for (i in 0 until arr.length()) {
                val u = arr.getJSONObject(i)
                val did = u.optString("did")
                val h = u.optString("handle")
                out.add(
                    FollowUser(
                        id = did.ifBlank { h },
                        handle = "@$h",
                        displayName = u.optString("displayName").ifBlank { h },
                        avatar = u.optString("avatar").ifBlank { null },
                        profileUrl = if (h.isNotBlank()) "https://bsky.app/profile/$h" else null
                    )
                )
            }
            cursor = if (json.has("cursor")) json.optString("cursor").ifBlank { null } else null
            pages++
        } while (cursor != null && pages < MAX_PAGES)
        return out to (pages >= MAX_PAGES && cursor != null)
    }

    private fun getJson(url: String): JSONObject? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 15000; conn.readTimeout = 15000
            conn.setRequestProperty("User-Agent", "SosyalPanel/1.0")
            if (conn.responseCode != 200) return null
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
