package com.vltv.play

import android.content.Context
import com.vltv.play.data.VpnInterceptor
import okhttp3.Dns
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---------------------
// Modelos de Dados (TODOS MANTIDOS INTEGRALMENTE)
// ---------------------

data class XtreamLoginResponse(val user_info: UserInfo?, val server_info: ServerInfo?)
data class UserInfo(val username: String?, val status: String?, val exp_date: String?)
data class ServerInfo(val url: String?, val port: String?, val server_protocol: String?)

data class LiveCategory(val category_id: String, val category_name: String) {
    val id: String get() = category_id
    val name: String get() = category_name
}

data class LiveStream(val stream_id: Int, val name: String, val stream_icon: String?, val epg_channel_id: String?) {
    val id: Int get() = stream_id
    val icon: String? get() = stream_icon
}

data class VodStream(
    val stream_id: Int,
    val name: String,
    val title: String?,
    val stream_icon: String?,
    val container_extension: String?,
    val rating: String?
) {
    val id: Int get() = stream_id
    val icon: String? get() = stream_icon
    val extension: String? get() = container_extension
}

data class SeriesStream(
    val series_id: Int,
    val name: String,
    val cover: String?,
    val rating: String?
) {
    val id: Int get() = series_id
    val icon: String? get() = cover
}

data class SeriesInfoResponse(val episodes: Map<String, List<EpisodeStream>>?)
data class EpisodeStream(val id: String, val title: String, val container_extension: String?, val season: Int, val episode_num: Int, val info: EpisodeInfo?)
data class EpisodeInfo(val plot: String?, val duration: String?, val movie_image: String?)

data class VodInfoResponse(val info: VodInfoData?)
data class VodInfoData(val plot: String?, val genre: String?, val director: String?, val cast: String?, val releasedate: String?, val rating: String?, val movie_image: String?)

data class EpgWrapper(val epg_listings: List<EpgResponseItem>?)
data class EpgResponseItem(val id: String?, val epg_id: String?, val title: String?, val lang: String?, val start: String?, val end: String?, val description: String?, val channel_id: String?, val start_timestamp: String?, val stop_timestamp: String?, val stop: String?)

// ---------------------
// Interface Retrofit (TODAS AS FUNÇÕES ORIGINAIS MANTIDAS)
// ---------------------

interface XtreamService {
    @GET("player_api.php")
    fun login(@Query("username") user: String, @Query("password") pass: String): Call<XtreamLoginResponse>

    @GET("player_api.php")
    fun getLiveCategories(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_live_categories"): Call<List<LiveCategory>>

    @GET("player_api.php")
    fun getLiveStreams(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_live_streams", @Query("category_id") categoryId: String): Call<List<LiveStream>>

    @GET("player_api.php")
    fun getVodCategories(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_vod_categories"): Call<List<LiveCategory>>

    @GET("player_api.php")
    fun getVodStreams(
        @Query("username") user: String, 
        @Query("password") pass: String, 
        @Query("action") action: String = "get_vod_streams", 
        @Query("category_id") categoryId: String = "0"
    ): Call<List<VodStream>>

    @GET("player_api.php")
    fun getAllVodStreams(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_vod_streams"): Call<List<VodStream>>

    @GET("player_api.php")
    fun getVodInfo(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_vod_info", @Query("vod_id") vodId: Int): Call<VodInfoResponse>

    @GET("player_api.php")
    fun getSeriesCategories(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_series_categories"): Call<List<LiveCategory>>

    @GET("player_api.php")
    fun getSeries(
        @Query("username") user: String, 
        @Query("password") pass: String, 
        @Query("action") action: String = "get_series", 
        @Query("category_id") categoryId: String = "0"
    ): Call<List<SeriesStream>>

    @GET("player_api.php")
    fun getAllSeries(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_series"): Call<List<SeriesStream>>

    @GET("player_api.php")
    fun getSeriesInfoV2(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_series_info", @Query("series_id") seriesId: Int): Call<SeriesInfoResponse>

    @GET("player_api.php")
    fun getShortEpg(@Query("username") user: String, @Query("password") pass: String, @Query("action") action: String = "get_short_epg", @Query("stream_id") streamId: String, @Query("limit") limit: Int = 2): Call<EpgWrapper>
}

// ---------------------
// Objeto XtreamApi (ATUALIZADO PARA SUPORTAR MULTI-DNS E VPN)
// ---------------------

object XtreamApi {
    private var retrofit: Retrofit? = null
    private var baseUrl: String = "http://placeholder.com/" 
    private var okHttpClient: OkHttpClient? = null

    /**
     * Inicializa o cliente com o interceptor de VPN. 
     * Deve ser chamado uma vez no início do app (Ex: Application ou MainActivity).
     */
    fun init(context: Context) {
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .dns(Dns.SYSTEM) // Utiliza o DNS do sistema para evitar bloqueios de resolução simples
            .addInterceptor(VpnInterceptor(context)) // Injeta a segurança de VPN
            .build()
        retrofit = null // Força recriação para aplicar o novo cliente
    }

    fun setBaseUrl(newUrl: String) {
        val formattedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (baseUrl != formattedUrl) {
            baseUrl = formattedUrl
            retrofit = null
        }
    }

    val service: XtreamService get() {
        if (retrofit == null) {
            val builder = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
            
            // Se o init foi chamado, usa o cliente com VPN, senão usa um padrão seguro
            val client = okHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .dns(Dns.SYSTEM)
                .build()
                
            retrofit = builder.client(client).build()
        }
        return retrofit!!.create(XtreamService::class.java)
    }
}
