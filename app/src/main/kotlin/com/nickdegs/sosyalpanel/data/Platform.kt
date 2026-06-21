package com.nickdegs.sosyalpanel.data

import androidx.compose.ui.graphics.Color

// iOS SocialPlatform karşılığı — 13 platform, marka rengi, uygulama şeması, web URL'i.
enum class Platform(
    val id: String,
    val displayName: String,
    val brandColor: Color,
    val appScheme: String,        // uygulama derin bağlantısı
    val webBase: String,          // profil web tabanı (@username eklenir)
    val packageName: String       // Android paket adı (canOpenURL karşılığı)
) {
    INSTAGRAM("instagram", "Instagram", Color(0xFFE4405F), "instagram://user?username=", "https://instagram.com/", "com.instagram.android"),
    YOUTUBE("youtube", "YouTube", Color(0xFFFF0000), "youtube://", "https://youtube.com/@", "com.google.android.youtube"),
    X("x", "X", Color(0xFF000000), "twitter://user?screen_name=", "https://x.com/", "com.twitter.android"),
    FACEBOOK("facebook", "Facebook", Color(0xFF1877F2), "fb://facewebmodal/f?href=", "https://facebook.com/", "com.facebook.katana"),
    THREADS("threads", "Threads", Color(0xFF000000), "barcelona://", "https://threads.net/@", "com.instagram.barcelona"),
    PINTEREST("pinterest", "Pinterest", Color(0xFFE60023), "pinterest://", "https://pinterest.com/", "com.pinterest"),
    TIKTOK("tiktok", "TikTok", Color(0xFF000000), "tiktok://", "https://tiktok.com/@", "com.zhiliaoapp.musically"),
    REDDIT("reddit", "Reddit", Color(0xFFFF4500), "reddit://", "https://reddit.com/user/", "com.reddit.frontpage"),
    TUMBLR("tumblr", "Tumblr", Color(0xFF36465D), "tumblr://", "https://tumblr.com/", "com.tumblr"),
    VK("vk", "VK", Color(0xFF0077FF), "vk://", "https://vk.com/", "com.vkontakte.android"),
    BLUESKY("bluesky", "Bluesky", Color(0xFF0085FF), "bluesky://", "https://bsky.app/profile/", "xyz.blueskyweb.app"),
    TELEGRAM("telegram", "Telegram", Color(0xFF26A5E4), "tg://resolve?domain=", "https://t.me/", "org.telegram.messenger"),
    DISCORD("discord", "Discord", Color(0xFF5865F2), "discord://", "https://discord.com/", "com.discord");

    // SF Symbol yerine Material'da kendi marka rozetimizi PlatformBadge çiziyor.
    companion object {
        fun from(id: String): Platform = entries.firstOrNull { it.id == id } ?: INSTAGRAM
    }
}
