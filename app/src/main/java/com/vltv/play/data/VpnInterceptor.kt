package com.vltv.play.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.NetworkInterface

/**
 * 🛡️ INTERCEPTOR DE VPN (ROBUSTO)
 * Este arquivo verifica se o usuário está usando VPN antes de permitir a requisição à API.
 */
class VpnInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Verificação em tempo real
        if (isVpnActive()) {
            // Lançamos uma exceção para o OkHttp interromper a chamada
            throw IOException("VPN_DETECTED: Conexão protegida bloqueada.")
        }

        // 2. Se não houver VPN, a requisição segue normalmente
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "VLTV-PLAYER-PRO-V1")
            .build()

        return chain.proceed(request)
    }

    /**
     * Lógica inteligente de detecção de VPN
     */
    private fun isVpnActive(): Boolean {
        return try {
            // Método A: Checagem por ConnectivityManager (Padrão Android)
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            val hasVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false

            // Método B: Checagem por Interfaces de Rede (Mais agressivo/robusto)
            // VPNs geralmente criam interfaces como 'tun0', 'ppp' ou 'tap'
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var interfaceVpnFound = false
            
            if (interfaces != null) {
                for (networkInterface in interfaces.iterator()) {
                    val name = networkInterface.name.lowercase()
                    if (name.contains("tun") || name.contains("ppp") || name.contains("tap")) {
                        interfaceVpnFound = true
                        break
                    }
                }
            }

            hasVpn || interfaceVpnFound
        } catch (e: Exception) {
            false
        }
    }
}
