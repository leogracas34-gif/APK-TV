package com.vltv.play.data

import android.content.Context
import androidx.room.*

// ==========================================
// 🚀 ENTIDADES (TABELAS)
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
// 🚀 DAO (INTERFACE DE COMANDOS)
// ==========================================

@Dao
interface StreamDao {
    // Gravação em Massa (Para o Login Automático)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveStreams(streams: List<LiveStreamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVodStreams(streams: List<VodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeriesStreams(series: List<SeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    // Consultas Rápidas (Utilizadas na LiveTvActivity)
    @Query("SELECT * FROM categories WHERE type = :type")
    suspend fun getCategoriesByType(type: String): List<CategoryEntity>

    @Query("SELECT * FROM live_streams")
    suspend fun getAllLiveStreams(): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE category_id = :catId")
    suspend fun getLiveStreamsByCategory(catId: String): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE category_id = :catId")
    suspend fun getLiveByCategory(catId: String): List<LiveStreamEntity>

    @Query("SELECT * FROM vod_streams ORDER BY added DESC")
    suspend fun getAllVods(): List<VodEntity>

    // ✅ ADICIONADO: Consulta para VodActivity parar de dar erro
    @Query("SELECT * FROM vod_streams WHERE category_id = :catId")
    suspend fun getVodStreamsByCategory(catId: String): List<VodEntity>

    // ✅ ADICIONADO: Buscas individuais para DetailsActivity resolver erro de compilação
    @Query("SELECT * FROM vod_streams WHERE stream_id = :id LIMIT 1")
    suspend fun getVodById(id: Int): VodEntity?

    @Query("SELECT * FROM series_streams WHERE series_id = :id LIMIT 1")
    suspend fun getSeriesById(id: Int): SeriesEntity?

    // ✅ ADICIONADO: Consultas para SeriesActivity (Para uso futuro)
    @Query("SELECT * FROM series_streams")
    suspend fun getAllSeries(): List<SeriesEntity>

    @Query("SELECT * FROM series_streams WHERE category_id = :catId")
    suspend fun getSeriesByCategory(catId: String): List<SeriesEntity>

    // Limpeza Inteligente (Evita travamentos)
    @Query("DELETE FROM live_streams")
    suspend fun clearLive()

    @Query("DELETE FROM vod_streams")
    suspend fun clearVod()

    @Query("DELETE FROM series_streams")
    suspend fun clearSeries()

    // Perfis
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM user_profiles")
    suspend fun getAllProfiles(): List<ProfileEntity>
}

// ==========================================
// 🚀 ENGINE DO BANCO (SINGLETON)
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
    version = 1, // Começando como 1 para o novo projeto
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
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
                // Ativa o modo de gravação rápida para não travar o app
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
