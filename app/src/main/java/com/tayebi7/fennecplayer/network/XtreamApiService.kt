package com.tayebi7.fennecplayer.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. نماذج البيانات (Models)
data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: UserInfo?,
    @SerializedName("server_info") val serverInfo: ServerInfo?
)

data class UserInfo(
    val username: String?,
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
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String
)

data class LiveStreamModel(
    @SerializedName("stream_id") val streamId: Int,
    val name: String,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("category_id") val categoryId: String?
)

data class VodStreamModel(
    @SerializedName("stream_id") val streamId: Int,
    val name: String,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("container_extension") val containerExtension: String?
)

data class SeriesModel(
    @SerializedName("series_id") val seriesId: Int,
    val name: String,
    val cover: String?,
    @SerializedName("category_id") val categoryId: String?
)

// 2. واجهة الاتصال بالسيرفر (API Interface)
interface XtreamApiService {

    // تسجيل الدخول والتحقق من صلاحية الاشتراك
    @GET("player_api.php")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<XtreamAuthResponse>

    // جلب أقسام القنوات المباشرة
    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): Response<List<CategoryModel>>

    // جلب القنوات المباشرة
    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): Response<List<LiveStreamModel>>

    // جلب أقسام الأفلام (VOD)
    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): Response<List<CategoryModel>>

    // جلب الأفلام (VOD)
    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null
    ): Response<List<VodStreamModel>>

    // جلب أقسام المسلسلات (Series)
    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): Response<List<CategoryModel>>

    // جلب المسلسلات (Series)
    @GET("player_api.php")
    suspend fun getSeries(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null
    ): Response<List<SeriesModel>>

    companion object {
        fun create(baseUrl: String): XtreamApiService {
            val cleanUrl = baseUrl.trim()
            val formattedUrl = if (cleanUrl.endsWith("/")) cleanUrl else "$cleanUrl/"
            return Retrofit.Builder()
                .baseUrl(formattedUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(XtreamApiService::class.java)
        }

        // بناء رابط تشغيل القناة المباشرة (Live TV)
        fun buildLiveStreamUrl(
            baseUrl: String,
            username: String,
            password: String,
            streamId: Int,
            extension: String = "ts"
        ): String {
            val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
            return "$cleanBaseUrl/live/$username/$password/$streamId.$extension"
        }

        // بناء رابط تشغيل الفيلم (Movie / VOD)
        fun buildMovieUrl(
            baseUrl: String,
            username: String,
            password: String,
            streamId: Int,
            extension: String = "mp4"
        ): String {
            val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
            return "$cleanBaseUrl/movie/$username/$password/$streamId.$extension"
        }
    }
}
