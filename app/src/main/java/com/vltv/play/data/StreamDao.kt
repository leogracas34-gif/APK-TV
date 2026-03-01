package com.vltv.play.data

import androidx.room.*

/**
 * 🛰️ STREAM DAO (DATA ACCESS OBJECT)
 * Este é o "cérebro" das consultas SQL do seu aplicativo.
 * Ele permite carregar canais e filmes instantaneamente do banco vltv_smart_db.
 */
@Dao
interface StreamDao {

    // --- CATEGORIAS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY category_name ASC")
    suspend fun getCategoriesByType(type: String): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun clearCategories()


    // --- CANAIS AO VIVO (LIVE) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveStreams(streams: List<LiveStreamEntity>)

    @Query("SELECT * FROM live_streams ORDER BY name ASC")
    suspend fun getAllLiveStreams(): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE category_id = :catId ORDER BY name ASC")
    suspend fun getLiveStreamsByCategory(catId: String): List<LiveStreamEntity>

    @Query("DELETE FROM live_streams")
    suspend fun clearLive()


    // --- FILMES (VOD) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVodStreams(streams: List<VodEntity>)

    @Query("SELECT * FROM vod_streams ORDER BY added DESC")
    suspend fun getAllVods(): List<VodEntity>

    @Query("SELECT * FROM vod_streams WHERE category_id = :catId ORDER BY added DESC")
    suspend fun getVodsByCategory(catId: String): List<VodEntity>

    @Query("DELETE FROM vod_streams")
    suspend fun clearVod()


    // --- SÉRIES ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeriesStreams(series: List<SeriesEntity>)

    @Query("SELECT * FROM series_streams ORDER BY name ASC")
    suspend fun getAllSeries(): List<SeriesEntity>

    @Query("SELECT * FROM series_streams WHERE category_id = :catId ORDER BY name ASC")
    suspend fun getSeriesByCategory(catId: String): List<SeriesEntity>

    @Query("DELETE FROM series_streams")
    suspend fun clearSeries()
}
