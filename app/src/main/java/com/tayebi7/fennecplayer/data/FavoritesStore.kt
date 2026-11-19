package com.tayebi7.fennecplayer.data

import android.content.Context

/** المفضلة لكل قائمة تشغيل، تُحفظ كمجموعة مفاتيح "TYPE:id". */
object FavoritesStore {
    private fun prefs(c: Context) = c.getSharedPreferences("fennec_favs", Context.MODE_PRIVATE)
    private fun key(playlistId: String) = "fav_$playlistId"
    fun keyOf(e: Entry) = "${e.type.name}:${e.id}"

    fun keys(c: Context, playlistId: String): Set<String> =
        prefs(c).getStringSet(key(playlistId), emptySet()) ?: emptySet()

    fun isFavorite(c: Context, playlistId: String, e: Entry) = keyOf(e) in keys(c, playlistId)

    /** يعيد true إذا أصبح العنصر مفضلاً. */
    fun toggle(c: Context, playlistId: String, e: Entry): Boolean {
        val set = keys(c, playlistId).toMutableSet()
        val now = if (!set.remove(keyOf(e))) { set.add(keyOf(e)); true } else false
        prefs(c).edit().putStringSet(key(playlistId), set).apply()
        return now
    }

    fun clear(c: Context, playlistId: String) = prefs(c).edit().remove(key(playlistId)).apply()
    fun clearAll(c: Context) = prefs(c).edit().clear().apply()
}
