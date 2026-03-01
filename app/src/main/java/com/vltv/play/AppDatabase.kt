package com.vltv.play.data

import android.content.Context
import androidx.room.*

// ==========================================
// 🚀 ENTIDADES (TABELAS) - MANTIDAS INTEGRALMENTE
// ==========================================

@Entity(tableName = "user_profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var name: String,
    var imageUrl: String? = null,
    val isKids: Boolean = false
)

@Entity(tableName = "live_streams", indices = [Index(value = ["category_id"]), Index(value = ["name"])])
data class LiveStreamEntity(
    @PrimaryKey val stream_id: Int,
    val name: String,
    val stream_icon: String?,
    val epg_channel_id: String?,
    val category_id: String
)

@Entity(tableName = "vod_streams", indices = [Index(value = ["category_id"]), Index(value = ["name"])])
data class VodEntity(
    @PrimaryKey val stream_id: Int,
    val name: String,
    val title: String?,
    val stream_icon: String?,
    val container_extension: String?,
    val rating: String?,
    val category_id: String,
    val added: Long,
    val logo_url: String? = null
)

@Entity(tableName = "series_streams", indices = [Index(value = ["category_id"]), Index(value = ["name"])])
data class SeriesEntity(
    @PrimaryKey val series_id: Int,
    val name: String,
    val cover: String?,
    val rating: String?,
    val category_id: String,
    val last_modified: Long,
    val logo_url: String? = null
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val category_id: String,
    val category_name: String,
    val type: String // "live", "vod", "series"
)

@Entity(tableName = "watch_history", primaryKeys = ["stream_id", "profile_name"])
data class WatchHistoryEntity(
    val stream_id: Int,
    val profile_name: String,
    val name: String,
    val icon: String?,
    val last_position: Long,
    val duration: Long,
    val is_series: Boolean,
    val timestamp: Long
)

// ==========================================
// 🚀 DAO (INTERFACE DE COMANDOS ATUALIZADA)
// ==========================================

@Dao
interface StreamDao {
    // Gravação em Massa (MANTIDO)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveStreams(streams: List<LiveStreamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVodStreams(streams: List<VodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeriesStreams(series: List<SeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    // ✅ CONSULTAS PARA LIVETVACTIVITY (ADICIONADAS PARA CORRIGIR ERRO)
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY category_name ASC")
    suspend fun getCategoriesByType(type: String): List<CategoryEntity>

    @Query("SELECT * FROM live_streams ORDER BY name ASC")
    suspend fun getAllLiveStreams(): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE category_id = :catId ORDER BY name ASC")
    suspend fun getLiveStreamsByCategory(catId: String): List<LiveStreamEntity>

    // Consultas Rápidas Originais (MANTIDO)
    @Query("SELECT * FROM live_streams WHERE category_id = :catId")
    suspend fun getLiveByCategory(catId: String): List<LiveStreamEntity>

    @Query("SELECT * FROM vod_streams ORDER BY added DESC")
    suspend fun getAllVods(): List<VodEntity>

    // ✅ CONSULTAS PARA VOD/SERIES (ADICIONADAS)
    @Query("SELECT * FROM vod_streams WHERE category_id = :catId ORDER BY added DESC")
    suspend fun getVodsByCategory(catId: String): List<VodEntity>

    @Query("SELECT * FROM series_streams WHERE category_id = :catId ORDER BY name ASC")
    suspend fun getSeriesByCategory(catId: String): List<SeriesEntity>

    // Limpeza Inteligente (MANTIDO)
    @Query("DELETE FROM live_streams")
    suspend fun clearLive()

    @Query("DELETE FROM vod_streams")
    suspend fun clearVod()

    @Query("DELETE FROM series_streams")
    suspend fun clearSeries()
    
    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // Perfis (MANTIDO)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM user_profiles")
    suspend fun getAllProfiles(): List<ProfileEntity>
}

// ==========================================
// 🚀 ENGINE DO BANCO (SINGLETON) - ATUALIZADO
// ==========================================

@Database(
    entities = [
        LiveStreamEntity::class, 
        VodEntity::class, 
        SeriesEntity::class, 
        CategoryEntity::class, 
        WatchHistoryEntity::class,
        ProfileEntity::class
    ], 
    version = 2, // ✅ Subimos para 2 para reconhecer as novas funções e tabelas
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Aqui o Database apenas diz que o StreamDao existe. 
    // Como o Kotlin agora sabe onde ele está, não haverá redeclaração.
    abstract fun streamDao(): StreamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vltv_smart_db"
                )
                .fallbackToDestructiveMigration()
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
