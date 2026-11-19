package com.tayebi7.fennecplayer.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** قوائم التشغيل المحفوظة (عدة قوائم) + القائمة المفعّلة. التخزين محلي على الجهاز فقط. */
object PlaylistStore {
    const val PREFS = "fennec_prefs"
    private const val KEY_LIST = "playlists_json"
    private const val KEY_ACTIVE = "active_playlist_id"
    private val gson = Gson()

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(context: Context): List<Playlist> {
        val p = prefs(context)
        val json = p.getString(KEY_LIST, null)
        if (json != null) {
            return runCatching {
                gson.fromJson<List<Playlist>>(json, object : TypeToken<List<Playlist>>() {}.type)
            }.getOrNull().orEmpty().filter { it.isValid }
        }
        // ترحيل القائمة الوحيدة من الإصدارات القديمة
        val legacy = migrateLegacy(context)
        return listOfNotNull(legacy)
    }

    private fun migrateLegacy(context: Context): Playlist? {
        val p = prefs(context)
        val type = runCatching { PlaylistType.valueOf(p.getString("playlist_type", "XTREAM") ?: "XTREAM") }
            .getOrDefault(PlaylistType.XTREAM)
        val pl = Playlist(
            name = p.getString("playlist_name", "Fennec TV") ?: "Fennec TV",
            type = type,
            server = p.getString("server_url", "") ?: "",
            username = p.getString("username", "") ?: "",
            password = p.getString("password", "") ?: "",
            m3uUrl = p.getString("m3u_url", "") ?: ""
        ).takeIf { it.isValid } ?: return null
        persist(context, listOf(pl), pl.id)
        return pl
    }

    private fun persist(context: Context, list: List<Playlist>, activeId: String?) {
        prefs(context).edit()
            .putString(KEY_LIST, gson.toJson(list))
            .putString(KEY_ACTIVE, activeId)
            .apply()
    }

    /** القائمة المفعّلة حالياً (أو الأولى إن لم تُحدد). */
    fun load(context: Context): Playlist? {
        val list = all(context)
        val id = prefs(context).getString(KEY_ACTIVE, null)
        return list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

    /** يضيف القائمة أو يحدّثها ثم يجعلها المفعّلة. */
    fun save(context: Context, playlist: Playlist) {
        val list = all(context).filter { it.id != playlist.id } + playlist
        persist(context, list, playlist.id)
    }

    fun setActive(context: Context, id: String) {
        persist(context, all(context), id)
    }

    fun remove(context: Context, id: String) {
        val list = all(context)
        list.firstOrNull { it.id == id }?.let { removeLocalFile(it) }
        val rest = list.filter { it.id != id }
        val active = prefs(context).getString(KEY_ACTIVE, null)
        persist(context, rest, if (active == id) rest.firstOrNull()?.id else active)
        Favorites.clear(context, id)
    }

    fun clearAll(context: Context) {
        all(context).forEach { removeLocalFile(it) }
        prefs(context).edit().clear().apply()
        context.getSharedPreferences(Favorites.PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences(Resume.PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun removeLocalFile(p: Playlist) {
        if (p.type == PlaylistType.LOCAL) runCatching { File(p.m3uUrl).delete() }
    }

    fun localDir(context: Context): File = File(context.filesDir, "playlists").apply { mkdirs() }
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
