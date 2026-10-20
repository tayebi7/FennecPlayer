package com.tayebi7.fennecplayer.data

import android.content.Context
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tayebi7.fennecplayer.network.XtreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** مصدر البيانات الموحد: يقرأ Xtream أو M3U ويحتفظ بالنتيجة في الذاكرة. */
object Repo {

    private val mutex = Mutex()
    private var signature = ""
    private val cache = HashMap<ContentType, Catalog>()

    fun invalidate() {
        cache.clear()
        signature = ""
    }

    suspend fun catalog(context: Context, type: ContentType): Catalog = withContext(Dispatchers.IO) {
        val playlist = PlaylistStore.load(context) ?: return@withContext Catalog.EMPTY
        mutex.withLock {
            if (signature != playlist.signature) {
                cache.clear()
                signature = playlist.signature
            }
            cache[type]?.let { return@withLock it }
            when (playlist.type) {
                PlaylistType.XTREAM -> loadXtream(playlist, type).also { cache[type] = it }
                PlaylistType.M3U -> {
                    storeM3u(downloadM3u(playlist.m3uUrl))
                    cache[type] ?: Catalog.EMPTY
                }
            }
        }
    }

    /** يتحقق من صحة القائمة قبل حفظها. يعيد null عند النجاح أو رسالة الخطأ. */
    suspend fun verify(playlist: Playlist): String? = withContext(Dispatchers.IO) {
        try {
            when (playlist.type) {
                PlaylistType.XTREAM -> {
                    val res = XtreamApiService.create(playlist.server).login(playlist.username, playlist.password)
                    if (!res.isSuccessful) return@withContext "السيرفر رد بالخطأ HTTP ${res.code()}"
                    val info = res.body()?.userInfo
                    when {
                        info == null || info.auth == 0 -> "اسم المستخدم أو كلمة المرور غير صحيحة"
                        else -> null
                    }
                }
                PlaylistType.M3U -> {
                    val parsed = downloadM3u(playlist.m3uUrl)
                    val total = parsed.values.sumOf { it.entries.size }
                    if (total == 0) "الملف لا يحتوي على أي قنوات صالحة"
                    else mutex.withLock {
                        cache.clear()
                        signature = playlist.signature
                        storeM3u(parsed)
                        null
                    }
                }
            }
        } catch (e: Exception) {
            "تعذر الاتصال: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    suspend fun accountInfo(context: Context): AccountInfo? = withContext(Dispatchers.IO) {
        val p = PlaylistStore.load(context)?.takeIf { it.type == PlaylistType.XTREAM } ?: return@withContext null
        val info = XtreamApiService.create(p.server).login(p.username, p.password).body()?.userInfo
            ?: return@withContext null
        AccountInfo(
            status = info.status ?: "",
            expiry = formatExpiry(info.expDate),
            maxConnections = info.maxConnections ?: ""
        )
    }

    suspend fun episodes(context: Context, seriesId: String): List<Episode> = withContext(Dispatchers.IO) {
        val p = PlaylistStore.load(context) ?: return@withContext emptyList()
        val res = XtreamApiService.create(p.server).getSeriesInfo(p.username, p.password, seriesId)
        if (!res.isSuccessful) throw IOException("HTTP ${res.code()}")
        val root = JsonParser.parseString(res.body()?.string().orEmpty())
        if (!root.isJsonObject) return@withContext emptyList()
        val eps = root.asJsonObject.get("episodes") ?: return@withContext emptyList()

        val out = ArrayList<Episode>()
        fun addSeason(seasonKey: String, arr: JsonElement) {
            if (!arr.isJsonArray) return
            val season = seasonKey.toIntOrNull() ?: 1
            for (e in arr.asJsonArray) {
                if (!e.isJsonObject) continue
                val o = e.asJsonObject
                val id = o.str("id") ?: continue
                val num = o.str("episode_num")?.toIntOrNull() ?: (out.size + 1)
                val ext = o.str("container_extension")?.ifEmpty { null } ?: "mp4"
                val title = o.str("title")?.ifBlank { null } ?: "الحلقة $num"
                out.add(
                    Episode(
                        id = id, title = title, season = season, number = num,
                        url = XtreamApiService.buildEpisodeUrl(p.server, p.username, p.password, id, ext)
                    )
                )
            }
        }
        if (eps.isJsonObject) {
            for ((k, v) in eps.asJsonObject.entrySet()) addSeason(k, v)
        } else if (eps.isJsonArray) {
            eps.asJsonArray.forEachIndexed { i, v -> addSeason((i + 1).toString(), v) }
        }
        out.sortedWith(compareBy({ it.season }, { it.number }))
    }

    // ───────── Xtream ─────────
    private suspend fun loadXtream(p: Playlist, type: ContentType): Catalog {
        val api = XtreamApiService.create(p.server)
        val u = p.username
        val pw = p.password
        return when (type) {
            ContentType.LIVE -> {
                val cats = api.getLiveCategories(u, pw).list()
                val items = api.getLiveStreams(u, pw).list().map {
                    Entry(
                        id = it.streamId.toString(), name = it.name ?: "—", logo = it.streamIcon,
                        categoryId = it.categoryId ?: "", type = type,
                        url = XtreamApiService.buildLiveStreamUrl(p.server, u, pw, it.streamId)
                    )
                }
                build(cats.map { Category(it.categoryId ?: "", it.categoryName ?: "—") }, items)
            }
            ContentType.MOVIE -> {
                val cats = api.getVodCategories(u, pw).list()
                val items = api.getVodStreams(u, pw).list().map {
                    Entry(
                        id = it.streamId.toString(), name = it.name ?: "—", logo = it.streamIcon,
                        categoryId = it.categoryId ?: "", type = type,
                        url = XtreamApiService.buildMovieUrl(
                            p.server, u, pw, it.streamId, it.containerExtension?.ifEmpty { null } ?: "mp4"
                        )
                    )
                }
                build(cats.map { Category(it.categoryId ?: "", it.categoryName ?: "—") }, items)
            }
            ContentType.SERIES -> {
                val cats = api.getSeriesCategories(u, pw).list()
                val items = api.getSeries(u, pw).list().map {
                    Entry(
                        id = it.seriesId.toString(), name = it.name ?: "—", logo = it.cover,
                        categoryId = it.categoryId ?: "", type = type, url = null
                    )
                }
                build(cats.map { Category(it.categoryId ?: "", it.categoryName ?: "—") }, items)
            }
        }
    }

    private fun build(cats: List<Category>, items: List<Entry>): Catalog {
        val counts = items.groupingBy { it.categoryId }.eachCount()
        return Catalog(cats.map { it.copy(count = counts[it.id] ?: 0) }, items)
    }

    private fun <T> Response<List<T>>.list(): List<T> {
        if (!isSuccessful) throw IOException("HTTP ${code()}")
        return body().orEmpty()
    }

    // ───────── M3U ─────────
    private fun downloadM3u(url: String): Map<ContentType, Catalog> {
        val req = Request.Builder().url(url).build()
        XtreamApiService.httpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
            val body = res.body ?: throw IOException("ملف فارغ")
            return M3uParser.parse(body.charStream().buffered())
        }
    }

    private fun storeM3u(parsed: Map<ContentType, Catalog>) {
        parsed.forEach { (t, c) -> cache[t] = c }
    }

    // ───────── أدوات ─────────
    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString

    private fun formatExpiry(ts: String?): String {
        if (ts.isNullOrEmpty() || ts == "null") return "غير محدود"
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts.toLong() * 1000L))
        } catch (e: Exception) {
            "غير محدود"
        }
    }
}
