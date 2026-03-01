package com.vltv.play.data

import com.vltv.play.LiveStream
import com.vltv.play.VodStream
import com.vltv.play.SeriesStream
import com.vltv.play.LiveCategory

// ==========================================
// 🚀 MAPPERS: CONVERSÃO API -> DATABASE
// ==========================================

/**
 * Converte um canal (LiveStream) da API para a entidade do Banco de Dados.
 */
fun LiveStream.toEntity(categoryId: String): LiveStreamEntity {
    return LiveStreamEntity(
        stream_id = this.stream_id,
        name = this.name,
        stream_icon = this.stream_icon,
        epg_channel_id = this.epg_channel_id,
        category_id = categoryId
    )
}

/**
 * Converte um filme (VodStream) da API para a entidade do Banco de Dados.
 */
fun VodStream.toEntity(categoryId: String): VodEntity {
    return VodEntity(
        stream_id = this.stream_id,
        name = this.name,
        title = this.title,
        stream_icon = this.stream_icon,
        container_extension = this.container_extension,
        rating = this.rating,
        category_id = categoryId,
        added = System.currentTimeMillis() // Timestamp para ordenar os "Adicionados Recentemente"
    )
}

/**
 * Converte uma série (SeriesStream) da API para a entidade do Banco de Dados.
 */
fun SeriesStream.toEntity(categoryId: String): SeriesEntity {
    return SeriesEntity(
        series_id = this.series_id,
        name = this.name,
        cover = this.cover,
        rating = this.rating,
        category_id = categoryId,
        last_modified = System.currentTimeMillis()
    )
}

/**
 * Converte uma categoria da API para a entidade do Banco de Dados.
 * @param type Deve ser "live", "vod" ou "series".
 */
fun LiveCategory.toEntity(type: String): CategoryEntity {
    return CategoryEntity(
        category_id = this.category_id,
        category_name = this.category_name,
        type = type
    )
}
