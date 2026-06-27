package com.nickdegs.sosyalpanel.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

// Resmi veri içe aktarma — Instagram/TikTok "Verilerini İndir" (Download Your Data)
// dosyalarını CİHAZDA işler. Ağ yok, kazıma yok, şifre/login yok. Kullanıcının kendi
// resmi verisi olduğu için tamamen yasal. Takipçi listesi public API'de gelmediğinden
// "geri takip etmeyen" analizi IG/TikTok için bu yolla yapılır.
object DataImportService {

    suspend fun instagram(ctx: Context, followers: Uri, following: Uri): FollowAnalysis? =
        generic(ctx, followers, following, Platform.INSTAGRAM)

    suspend fun generic(ctx: Context, followers: Uri, following: Uri, platform: Platform): FollowAnalysis? =
        withContext(Dispatchers.IO) {
            val fr = readText(ctx, followers)?.let { parseList(it, platform) } ?: return@withContext null
            val fo = readText(ctx, following)?.let { parseList(it, platform) } ?: return@withContext null
            if (fr.isEmpty() && fo.isEmpty()) null else build(fr, fo)
        }

    suspend fun tiktok(ctx: Context, file: Uri): FollowAnalysis? = withContext(Dispatchers.IO) {
        val text = readText(ctx, file) ?: return@withContext null
        val root = runCatching { JSONTokener(text).nextValue() }.getOrNull() ?: return@withContext null
        val followers = ArrayList<FollowUser>()
        val following = ArrayList<FollowUser>()
        splitFollow(root, Ctx.NONE, followers, following)
        if (followers.isEmpty() && following.isEmpty()) null
        else build(dedupe(followers), dedupe(following))
    }

    // MARK: hesaplama
    private fun build(followers: List<FollowUser>, following: List<FollowUser>): FollowAnalysis {
        val fr = dedupe(followers); val fo = dedupe(following)
        val frIds = fr.map { it.id.lowercase() }.toHashSet()
        val foIds = fo.map { it.id.lowercase() }.toHashSet()
        return FollowAnalysis(
            notFollowingBack = fo.filter { it.id.lowercase() !in frIds },
            youDontFollowBack = fr.filter { it.id.lowercase() !in foIds },
            mutualCount = frIds.count { it in foIds },
            followerCount = fr.size,
            followingCount = fo.size,
            truncated = false
        )
    }

    private fun dedupe(users: List<FollowUser>): List<FollowUser> {
        val seen = HashSet<String>(); val out = ArrayList<FollowUser>()
        for (u in users) if (u.id.isNotBlank() && seen.add(u.id.lowercase())) out.add(u)
        return out
    }

    // MARK: Instagram-tarzı parse (JSON öncelik, HTML yedek)
    private fun parseList(text: String, platform: Platform): List<FollowUser> {
        val root = runCatching { JSONTokener(text).nextValue() }.getOrNull()
        if (root != null) {
            val out = ArrayList<FollowUser>()
            collectJson(root, platform, out)
            if (out.isNotEmpty()) return dedupe(out)
        }
        // HTML yedeği
        val html = parseHtml(text, platform)
        return dedupe(html)
    }

    private fun collectJson(any: Any?, platform: Platform, out: ArrayList<FollowUser>) {
        when (any) {
            is JSONObject -> {
                val sld = any.optJSONArray("string_list_data")
                if (sld != null && sld.length() > 0) {
                    val first = sld.optJSONObject(0)
                    val v = first?.optString("value").orEmpty()
                    if (v.isNotBlank()) {
                        out.add(makeUser(v, first?.optString("href"), platform)); return
                    }
                }
                userName(any)?.let { out.add(makeUser(it, any.optString("href"), platform)); return }
                val keys = any.keys()
                while (keys.hasNext()) collectJson(any.opt(keys.next()), platform, out)
            }
            is JSONArray -> for (i in 0 until any.length()) collectJson(any.opt(i), platform, out)
        }
    }

    // MARK: TikTok-tarzı (Follower List / Following List anahtarlarına göre ayır)
    private enum class Ctx { NONE, FOLLOWERS, FOLLOWING }

    private fun splitFollow(any: Any?, ctx: Ctx, followers: ArrayList<FollowUser>, following: ArrayList<FollowUser>) {
        when (any) {
            is JSONObject -> {
                val uname = userName(any)
                if (uname != null && ctx != Ctx.NONE) {
                    val fu = makeUser(uname, any.optString("Link"), Platform.TIKTOK)
                    if (ctx == Ctx.FOLLOWERS) followers.add(fu) else following.add(fu)
                    return
                }
                val keys = any.keys()
                while (keys.hasNext()) {
                    val k = keys.next(); val kl = k.lowercase()
                    val next = when {
                        kl.contains("fans") || (kl.contains("follower") && !kl.contains("following")) -> Ctx.FOLLOWERS
                        kl.contains("following") -> Ctx.FOLLOWING
                        else -> ctx
                    }
                    splitFollow(any.opt(k), next, followers, following)
                }
            }
            is JSONArray -> for (i in 0 until any.length()) splitFollow(any.opt(i), ctx, followers, following)
        }
    }

    private fun userName(o: JSONObject): String? {
        for (key in listOf("UserName", "Username", "userName", "username", "user_name")) {
            val v = o.optString(key)
            if (v.isNotBlank()) return v
        }
        return null
    }

    private fun makeUser(raw: String, href: String?, platform: Platform): FollowUser {
        val name = raw.trim().removePrefix("@")
        val profile = href?.takeIf { it.isNotBlank() } ?: defaultProfile(name, platform)
        return FollowUser(id = name, handle = "@$name", displayName = name, avatar = null, profileUrl = profile)
    }

    private fun defaultProfile(user: String, platform: Platform): String? = when (platform) {
        Platform.INSTAGRAM -> "https://www.instagram.com/$user"
        Platform.TIKTOK -> "https://www.tiktok.com/@$user"
        else -> null
    }

    // MARK: HTML yedeği
    private fun parseHtml(html: String, platform: Platform): List<FollowUser> {
        val regex = when (platform) {
            Platform.INSTAGRAM -> Regex("instagram\\.com/([A-Za-z0-9._]+)")
            Platform.TIKTOK -> Regex("tiktok\\.com/@([A-Za-z0-9._]+)")
            else -> Regex("(?:instagram|tiktok)\\.com/@?([A-Za-z0-9._]+)")
        }
        val skip = setOf("p", "explore", "reels", "stories", "accounts", "direct", "tv")
        val out = ArrayList<FollowUser>()
        for (m in regex.findAll(html)) {
            val u = m.groupValues[1]
            if (u.isNotBlank() && u.lowercase() !in skip) out.add(makeUser(u, null, platform))
        }
        return out
    }

    private fun readText(ctx: Context, uri: Uri): String? = runCatching {
        ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()
}
