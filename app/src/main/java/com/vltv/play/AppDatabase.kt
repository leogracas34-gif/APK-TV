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
    version = 2, // ✅ Versão 2 para sincronizar com o novo StreamDao.kt
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    // ✅ Removida a interface interna para eliminar o erro de Redeclaração.
    // O compilador agora buscará o StreamDao no arquivo separado.
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
