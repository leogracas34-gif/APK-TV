package com.vltv.play.data

import com.vltv.play.XtreamApi
import com.vltv.play.XtreamLoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainRepository(private val database: AppDatabase) {

    private val dao = database.streamDao()

    suspend fun performLoginAndSync(dns: String, user: String, pass: String): Result<XtreamLoginResponse?> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Configura o DNS dinamicamente (MANTIDO)
                XtreamApi.setBaseUrl(dns)

                // 2. Tenta o login na API (MANTIDO)
                val response = XtreamApi.service.login(user, pass).execute()

                if (response.isSuccessful && response.body()?.user_info?.status == "Active") {
                    
                    // 3. LIMPEZA INTELIGENTE: (MANTIDO)
                    // Limpamos o banco antes de popular para evitar conflitos de multi-dns
                    dao.clearLive()
                    dao.clearVod()
                    dao.clearSeries()

                    // 4. Sincronização Automática Completa
                    // Agora a lógica puxa todos os tipos de dados
                    syncContent(user, pass)

                    Result.success(response.body())
                } else {
                    Result.failure(Exception("Login inválido ou conta expirada"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun syncContent(user: String, pass: String) {
        // Busca todas as categorias e conteúdos e salva no Room
        // O Room com WRITE_AHEAD_LOGGING garante que isso não trave o app
        try {
            // --- SINCRIZAR VODS (FILMES) ---
            val vodsResponse = XtreamApi.service.getAllVodStreams(user, pass).execute()
            if (vodsResponse.isSuccessful) {
                val entities = vodsResponse.body()?.map { it.toEntity("0") } ?: emptyList()
                dao.insertVodStreams(entities)
            }
            
            // --- SINCRONIZAR CANAIS (LIVE) ---
            val liveResponse = XtreamApi.service.getLiveStreams(user, pass, action = "get_live_streams", categoryId = "0").execute()
            if (liveResponse.isSuccessful) {
                val liveEntities = liveResponse.body()?.map { it.toEntity("0") } ?: emptyList()
                dao.insertLiveStreams(liveEntities)
            }

            // --- SINCRONIZAR SÉRIES ---
            val seriesResponse = XtreamApi.service.getAllSeries(user, pass).execute()
            if (seriesResponse.isSuccessful) {
                val seriesEntities = seriesResponse.body()?.map { it.toEntity("0") } ?: emptyList()
                dao.insertSeriesStreams(seriesEntities)
            }

            // --- SINCRONIZAR CATEGORIAS ---
            val liveCatResponse = XtreamApi.service.getLiveCategories(user, pass).execute()
            if (liveCatResponse.isSuccessful) {
                val catEntities = liveCatResponse.body()?.map { it.toEntity("live") } ?: emptyList()
                dao.insertCategories(catEntities)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
