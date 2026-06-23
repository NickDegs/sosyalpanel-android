package com.nickdegs.sosyalpanel.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// SMS hesabına bağlı bulut senkronu (Supabase). JWT (auth.uid) → RLS ile kullanıcıya özel.
// iOS CloudSyncService.swift karşılığı. Cihazlar arası ana senkron; tam-değişim (düşük hacim).
object CloudSyncService {

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun headers(): Map<String, String>? {
        val token = AuthService.token ?: return null
        return mapOf(
            "apikey" to Backend.SUPABASE_ANON_KEY,
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
    }

    // MARK: - Yukarı (yerel → bulut)
    suspend fun syncUp(accounts: List<AccountWithSnapshots>) = withContext(Dispatchers.IO) {
        val uid = AuthService.userId ?: return@withContext
        val h = headers() ?: return@withContext
        // Önce kullanıcının bulut verisini temizle
        req("DELETE", "sp_snapshots?user_id=eq.$uid", null, h)
        req("DELETE", "sp_accounts?user_id=eq.$uid", null, h)

        for (acc in accounts) {
            val a = acc.account
            val accBody = JSONObject()
                .put("user_id", uid)
                .put("platform", a.platformId)
                .put("username", a.username)
                .put("sort_order", a.sortOrder)
            a.goalFollowers?.let { accBody.put("goal_followers", it) }
            val createdArr = reqJson(
                "POST", "sp_accounts", accBody,
                h + ("Prefer" to "return=representation")
            ) ?: continue
            val accID = (createdArr.optJSONObject(0))?.optString("id") ?: continue

            val snaps = acc.sorted.takeLast(90)
            if (snaps.isNotEmpty()) {
                val arr = JSONArray()
                for (s in snaps) {
                    val o = JSONObject()
                        .put("user_id", uid)
                        .put("account_id", accID)
                        .put("followers", s.followers)
                        .put("captured_at", iso.format(Date(s.capturedAt)))
                    s.following?.let { o.put("following", it) }
                    s.posts?.let { o.put("posts", it) }
                    arr.put(o)
                }
                req("POST", "sp_snapshots", arr.toString(), h)
            }
        }
    }

    // MARK: - Aşağı (bulut → yerel). Yerel boşsa geri yükler.
    suspend fun restore(repo: Repository): Int = withContext(Dispatchers.IO) {
        val h = headers() ?: return@withContext 0
        if (repo.count() > 0) return@withContext 0   // sadece yerel boşsa geri yükle
        val accs = reqJson(
            "GET", "sp_accounts?select=id,platform,username,goal_followers,sort_order", null, h
        ) ?: return@withContext 0

        var count = 0
        for (i in 0 until accs.length()) {
            val a = accs.optJSONObject(i) ?: continue
            val pr = a.optString("platform").ifBlank { continue }
            val un = a.optString("username").ifBlank { continue }
            val accID = a.optString("id").ifBlank { continue }
            val account = TrackedAccount(
                platformId = normalizePlatform(pr),
                username = un,
                sortOrder = a.optInt("sort_order", 0),
                goalFollowers = if (a.isNull("goal_followers")) null else a.optInt("goal_followers")
            )
            val newId = repo.insertFullAccount(account)
            count++

            val snaps = reqJson(
                "GET",
                "sp_snapshots?account_id=eq.$accID&select=followers,following,posts,captured_at",
                null, h
            ) ?: continue
            for (j in 0 until snaps.length()) {
                val s = snaps.optJSONObject(j) ?: continue
                val ts = runCatching { iso.parse(s.optString("captured_at")) }.getOrNull()?.time
                    ?: System.currentTimeMillis()
                repo.insertSnapshotRaw(
                    MetricSnapshot(
                        accountId = newId,
                        followers = s.optInt("followers", 0),
                        following = if (s.isNull("following")) null else s.optInt("following"),
                        posts = if (s.isNull("posts")) null else s.optInt("posts"),
                        capturedAt = ts
                    )
                )
            }
        }
        count
    }

    // iOS "twitter" → Android "x" eşlemesi (cihazlar arası uyum).
    private fun normalizePlatform(raw: String): String = when (raw) {
        "twitter" -> "x"
        else -> raw
    }

    // --- HTTP ---
    private fun req(method: String, path: String, body: String?, h: Map<String, String>): Boolean {
        val conn = URL("${Backend.SUPABASE_URL}/rest/v1/$path").openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = method
            conn.connectTimeout = 15000; conn.readTimeout = 30000
            h.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun reqJson(method: String, path: String, body: JSONObject?, h: Map<String, String>): JSONArray? {
        val conn = URL("${Backend.SUPABASE_URL}/rest/v1/$path").openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = method
            conn.connectTimeout = 15000; conn.readTimeout = 30000
            h.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            if (code !in 200..299) return null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            runCatching { JSONArray(text) }.getOrNull()
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
