package com.tayebi7.fennecplayer.data

import android.content.Context
import android.net.Uri

object PlaylistStore {
    const val PREFS = "fennec_prefs"

    fun load(context: Context): Playlist? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val type = runCatching {
            PlaylistType.valueOf(p.getString("playlist_type", "XTREAM") ?: "XTREAM")
        }.getOrDefault(PlaylistType.XTREAM)
        val playlist = Playlist(
            name = p.getString("playlist_name", "Fennec TV") ?: "Fennec TV",
            type = type,
            server = p.getString("server_url", "") ?: "",
            username = p.getString("username", "") ?: "",
            password = p.getString("password", "") ?: "",
            m3uUrl = p.getString("m3u_url", "") ?: ""
        )
        return playlist.takeIf { it.isValid }
    }

    fun save(context: Context, playlist: Playlist) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("playlist_name", playlist.name)
            .putString("playlist_type", playlist.type.name)
            .putString("server_url", playlist.server)
            .putString("username", playlist.username)
            .putString("password", playlist.password)
            .putString("m3u_url", playlist.m3uUrl)
            .apply()
    }
}

object UrlUtils {
    /** يضيف http:// عند غيابه ويحذف أي مسار api أو معاملات من رابط السيرفر. */
    fun normalizeServer(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        if (!s.contains("://")) s = "http://$s"
        s = s.substringBefore("#").substringBefore("?")
        for (suffix in listOf("/player_api.php", "/get.php", "/panel_api.php", "/xmltv.php")) {
            if (s.endsWith(suffix, ignoreCase = true)) s = s.dropLast(suffix.length)
        }
        return s.trimEnd('/')
    }

    fun normalizeUrl(raw: String): String {
        val s = raw.trim()
        return if (s.isNotEmpty() && !s.contains("://")) "http://$s" else s
    }

    /** إذا كان رابط M3U من نوع get.php?username=..&password=.. نستخرج بيانات Xtream منه. */
    fun xtreamFromM3u(url: String): Triple<String, String, String>? {
        val uri = Uri.parse(normalizeUrl(url))
        val path = uri.path.orEmpty()
        if (!path.endsWith("get.php", ignoreCase = true)) return null
        val user = uri.getQueryParameter("username")
        val pass = uri.getQueryParameter("password")
        if (user.isNullOrEmpty() || pass.isNullOrEmpty()) return null
        return Triple(normalizeServer(url), user, pass)
    }
}
