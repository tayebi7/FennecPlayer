package com.tayebi7.fennecplayer.data

import android.content.Context

/** المفضلة لكل قائمة تشغيل ولكل نوع محتوى. */
object Favorites {
    const val PREFS = "fennec_favs"

    private fun key(playlistId: String, type: ContentType) = "$playlistId:${type.name}"

    fun ids(context: Context, playlistId: String, type: ContentType): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(key(playlistId, type), emptySet()).orEmpty().toSet()

    /** يبدّل الحالة ويعيد true إذا أصبح العنصر مفضلاً. */
    fun toggle(context: Context, playlistId: String, type: ContentType, id: String): Boolean {
        val set = ids(context, playlistId, type).toMutableSet()
        val nowFav = set.add(id).also { if (!it) set.remove(id) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(key(playlistId, type), set).apply()
        return nowFav
    }

    fun clear(context: Context, playlistId: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val e = p.edit()
        ContentType.values().forEach { e.remove(key(playlistId, it)) }
        e.apply()
    }
}

/** آخر قناة تم تشغيلها وموضع المتابعة للأفلام والحلقات. */
object Resume {
    const val PREFS = "fennec_resume"

    fun lastChannel(context: Context, playlistId: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last_$playlistId", null)

    fun setLastChannel(context: Context, playlistId: String, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("last_$playlistId", id).apply()
    }

    fun position(context: Context, url: String): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("pos_${url.hashCode()}", 0L)

    fun savePosition(context: Context, url: String, positionMs: Long, durationMs: Long) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        val key = "pos_${url.hashCode()}"
        // لا نحفظ البدايات القصيرة ولا النهايات (يعتبر الفيلم مكتملاً)
        if (positionMs < 10_000 || (durationMs > 0 && positionMs > durationMs - 15_000)) e.remove(key)
        else e.putLong(key, positionMs)
        e.apply()
    }
}

/** إعدادات التطبيق البسيطة. */
object AppSettings {
    private const val PREFS = "fennec_settings"

    /** 0 = ملائم، 1 = ملء، 2 = تكبير */
    fun aspect(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("aspect", 0)

    fun setAspect(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("aspect", mode).apply()
    }
}
