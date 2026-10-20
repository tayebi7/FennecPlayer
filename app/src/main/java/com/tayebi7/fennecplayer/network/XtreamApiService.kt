package com.tayebi7.fennecplayer.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ───────── النماذج (كل الحقول قابلة للـ null لأن السيرفرات تختلف في الاستجابة) ─────────
data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: UserInfo?,
    @SerializedName("server_info") val serverInfo: ServerInfo?
)

data class UserInfo(
    val username: String?,
    val auth: Int?,
    val status: String?,
    @SerializedName("exp_date") val expDate: String?,
    @SerializedName("max_connections") val maxConnections: String?
)

data class ServerInfo(
    val url: String?,
    val port: String?,
    @SerializedName("server_protocol") val protocol: String?
)

data class CategoryModel(
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("category_name") val categoryName: String?
)

data class LiveStreamModel(
    @SerializedName("stream_id") val streamId: Int,
    val name: String?,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("category_id") val categoryId: String?
)

data class VodStreamModel(
    @SerializedName("stream_id") val streamId: Int,
    val name: String?,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("container_extension") val containerExtension: String?
)

data class SeriesModel(
    @SerializedName("series_id") val seriesId: Int,
    val name: String?,
    val cover: String?,
    @SerializedName("category_id") val categoryId: String?
)

// ───────── واجهة الاتصال ─────────
interface XtreamApiService {

    @GET("player_api.php")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<XtreamAuthResponse>

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): Response<List<CategoryModel>>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): Response<List<LiveStreamModel>>

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): Response<List<CategoryModel>>

    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): Response<List<VodStreamModel>>

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): Response<List<CategoryModel>>

    @GET("player_api.php")
    suspend fun getSeries(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series"
    ): Response<List<SeriesModel>>

    /** تفاصيل مسلسل: تُقرأ يدوياً لأن شكل "episodes" يختلف بين السيرفرات. */
    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("series_id") seriesId: String,
        @Query("action") action: String = "get_series_info"
    ): Response<ResponseBody>

    companion object {
        const val USER_AGENT = "FennecPlayer/1.1 (Android)"

        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)   // قوائم القنوات قد تكون ضخمة
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder().header("User-Agent", USER_AGENT).build()
                    )
                }
                .build()
        }

        fun create(baseUrl: String): XtreamApiService {
            val cleanUrl = baseUrl.trim()
            val formattedUrl = if (cleanUrl.endsWith("/")) cleanUrl else "$cleanUrl/"
            return Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(XtreamApiService::class.java)
        }

        fun buildLiveStreamUrl(
            baseUrl: String, username: String, password: String,
            streamId: Int, extension: String = "ts"
        ): String = "${baseUrl.trim().removeSuffix("/")}/live/$username/$password/$streamId.$extension"

        fun buildMovieUrl(
            baseUrl: String, username: String, password: String,
            streamId: Int, extension: String = "mp4"
        ): String = "${baseUrl.trim().removeSuffix("/")}/movie/$username/$password/$streamId.$extension"

        fun buildEpisodeUrl(
            baseUrl: String, username: String, password: String,
            episodeId: String, extension: String = "mp4"
        ): String = "${baseUrl.trim().removeSuffix("/")}/series/$username/$password/$episodeId.$extension"
    }
}
