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
                // 1. Configura o DNS dinamicamente
                XtreamApi.setBaseUrl(dns)

                // 2. Tenta o login na API
                val response = XtreamApi.service.login(user, pass).execute()

                if (response.isSuccessful && response.body()?.user_info?.status == "Active") {
                    
                    // 3. LIMPEZA INTELIGENTE: Se o login deu certo, limpamos o banco antes de popular
                    // Isso evita conflitos entre servidores diferentes (Multi DNS)
                    dao.clearLive()
                    dao.clearVod()
                    dao.clearSeries()

                    // 4. Sincronização Automática (Exemplo com VODs)
                    // Aqui a lógica puxa os dados e já salva no banco versão 6
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
            val vodsResponse = XtreamApi.service.getAllVodStreams(user, pass).execute()
            if (vodsResponse.isSuccessful) {
                val entities = vodsResponse.body()?.map { it.toEntity("0") } ?: emptyList()
                dao.insertVodStreams(entities)
            }
            
            // Repetir a lógica para Live e Series...
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
