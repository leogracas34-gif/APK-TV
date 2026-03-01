package com.vltv.play

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.vltv.play.databinding.ActivityLoginBinding
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // SEUS 8 SERVIDORES XTREAM (MANTIDOS)
    private val SERVERS = listOf(
        "http://tvblack.shop",
        "http://redeinternadestiny.top",
        "http://fibercdn.sbs",
        "http://blackstartv.shop",
        "http://blackdns.shop",
        "http://ouropreto.top",
        "http://onwaveth.xyz:80",
        "http://blackdeluxe.shop"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 1. VERIFICAÇÃO SILENCIOSA (igual ao que funciona)
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("username", null)
        val savedPass = prefs.getString("password", null)
        val savedDns = prefs.getString("dns", null)

        if (!savedUser.isNullOrBlank() && !savedPass.isNullOrBlank() && !savedDns.isNullOrBlank()) {
            verificarEIniciar(savedDns!!, savedUser!!, savedPass!!)
            return
        }

        // ✅ 2. DESENHA TELA DE LOGIN
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MODO IMERSIVO (SEU CÓDIGO MANTIDO)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())

        setupTouchAndDpad()  // SUA UI PREMIUM MANTIDA

        binding.btnLogin.setOnClickListener {
            val user = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                iniciarLoginTurbo(user, pass)  // NOVA LÓGICA OTIMIZADA
            }
        }
    }

    // ✅ NOVA: Corrida de DNS (igual ao projeto que funciona)
    private fun iniciarLoginTurbo(user: String, pass: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val deferreds = SERVERS.map { server -> async { testarConexaoIndividual(server, user, pass) } }
                var dnsVencedor: String? = null
                val startTime = System.currentTimeMillis()
                
                while (System.currentTimeMillis() - startTime < 8000) {  // 8s max
                    val completed = deferreds.filter { it.isCompleted }
                    for (job in completed) {
                        val result = job.getCompleted()
                        if (result != null) {
                            dnsVencedor = result
                            break
                        }
                    }
                    if (dnsVencedor != null) break
                    delay(100)
                }

                deferreds.forEach { if (it.isActive) it.cancel() }

                if (dnsVencedor != null) {
                    salvarCredenciais(dnsVencedor!!, user, pass)
                    preCarregarConteudoInicial(dnsVencedor!!, user, pass)  // ✅ PRÉ-CARREGA
                    withContext(Dispatchers.Main) { startHomeActivity() }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Nenhum servidor disponível", Toast.LENGTH_LONG).show()
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                }
            }
        }
    }

    // ✅ NOVA: Teste individual (igual ao que funciona)
    private fun testarConexaoIndividual(baseUrl: String, user: String, pass: String): String? {
        val urlLimpa = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val apiLogin = "$urlLimpa/player_api.php?username=$user&password=$pass"

        return try {
            val request = Request.Builder().url(apiLogin).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.contains("user_info") && body.contains("server_info")) {
                        return urlLimpa
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ✅ NOVA: Pré-carrega conteúdo (igual ao que funciona)
    private suspend fun preCarregarConteudoInicial(dns: String, user: String, pass: String) {
        try {
            // [AQUI VOCÊ INSERE A LÓGICA DO SEU BANCO Room para VOD/Series]
            // Exemplo igual ao projeto que funciona:
            // val vodUrl = "$dns/player_api.php?username=$user&password=$pass&action=get_vod_streams"
            // ... parse JSON e salva no banco
        } catch (e: Exception) { }
    }

    // SEUS MÉTODOS MANTIDOS (setupTouchAndDpad, startHomeActivity, etc.)
    private fun setupTouchAndDpad() { /* SEU CÓDIGO PREMIUM */ }
    private fun startHomeActivity() { /* SEU CÓDIGO ATUAL */ }
    private fun verificarEIniciar(dns: String, user: String, pass: String) { /* LÓGICA SIMPLES */ }
    private fun salvarCredenciais(dns: String, user: String, pass: String) {
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("dns", dns)
            putString("username", user)
            putString("password", pass)
            apply()
        }
        XtreamApi.setBaseUrl("$dns/")
    }
}
