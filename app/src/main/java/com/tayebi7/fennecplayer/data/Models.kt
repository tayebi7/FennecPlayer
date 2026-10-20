package com.tayebi7.fennecplayer.data

enum class ContentType { LIVE, MOVIE, SERIES }

enum class PlaylistType { XTREAM, M3U }

data class Category(val id: String, val name: String, val count: Int = 0)

/** عنصر موحد (قناة / فيلم / مسلسل) سواء جاء من Xtream أو من ملف M3U. */
data class Entry(
    val id: String,
    val name: String,
    val logo: String?,
    val categoryId: String,
    val type: ContentType,
    /** رابط التشغيل المباشر. يكون null لمسلسلات Xtream (تُجلب حلقاتها لاحقاً). */
    val url: String?
)

data class Episode(
    val id: String,
    val title: String,
    val season: Int,
    val number: Int,
    val url: String
)

data class Catalog(val categories: List<Category>, val entries: List<Entry>) {
    companion object {
        val EMPTY = Catalog(emptyList(), emptyList())
    }
}

data class AccountInfo(
    val status: String,
    val expiry: String,
    val maxConnections: String
)

data class Playlist(
    val name: String,
    val type: PlaylistType,
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = ""
) {
    val isValid: Boolean
        get() = when (type) {
            PlaylistType.XTREAM -> server.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
            PlaylistType.M3U -> m3uUrl.isNotEmpty()
        }

    /** يتغير عند تغير مصدر البيانات فقط، ويستخدم لإبطال الكاش. */
    val signature: String
        get() = "$type|$server|$username|$password|$m3uUrl"
}
