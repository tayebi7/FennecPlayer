package com.tayebi7.fennecplayer.data

import android.content.Context

/** إعدادات خفيفة محلية: آخر قناة، مواضع المشاهدة، نسبة العرض، اللغة. */
object AppPrefs {
    private fun p(c: Context) = c.getSharedPreferences("fennec_app", Context.MODE_PRIVATE)

    // آخر قناة مباشرة لكل قائمة
    fun lastLive(c: Context, playlistId: String): String? = p(c).getString("last_live_$playlistId", null)
    fun setLastLive(c: Context, playlistId: String, entryId: String) {
        p(c).edit().putString("last_live_$playlistId", entryId).apply()
    }

    // متابعة المشاهدة: موضع آخر تشغيل لكل رابط
    fun position(c: Context, url: String): Long = p(c).getLong("pos_${url.hashCode()}", 0L)
    fun setPosition(c: Context, url: String, pos: Long) {
        p(c).edit().apply {
            if (pos <= 0L) remove("pos_${url.hashCode()}") else putLong("pos_${url.hashCode()}", pos)
        }.apply()
    }

    // نسبة العرض: 0 = ملاءمة، 1 = ملء، 2 = تكبير
    fun aspectMode(c: Context): Int = p(c).getInt("aspect", 0)
    fun setAspectMode(c: Context, v: Int) = p(c).edit().putInt("aspect", v).apply()

    // اللغة: "" = لغة الجهاز، أو ar / fr / en
    fun language(c: Context): String = p(c).getString("lang", "") ?: ""
    fun setLanguage(c: Context, v: String) = p(c).edit().putString("lang", v).apply()

    fun clear(c: Context) = p(c).edit().clear().apply()
}
