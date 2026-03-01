package com.vltv.play

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.vltv.play.data.AppDatabase
import com.vltv.play.data.MainRepository
import com.vltv.play.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    // Repositório para gerenciar a Database Inteligente
    private lateinit var repository: MainRepository

    // SEUS 6 SERVIDORES XTREAM (MANTIDOS INTEGRALMENTE)
    private val SERVERS = listOf(
        "http://tvblack.shop",
        "http://redeinternadestiny.top",
        "http://fibercdn.sbs",
        "http://vupro.shop",
        "http://blackdns.shop",
        "http://blackdeluxe.shop"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Banco de Dados e o Repositório
        val database = AppDatabase.getDatabase(this)
        repository = MainRepository(database)
        
        // Inicializa a API com a proteção de VPN e Camuflagem
        XtreamApi.init(this)

        val windowInsetsController =
        WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())

        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("username", null)
        val savedPass = prefs.getString("password", null)

        if (!savedUser.isNullOrBlank() && !savedPass.isNullOrBlank()) {
            startHomeActivity()
            return
        }

        // ✅ Config D-Pad TV com Animação Premium (MANTIDO)
        setupTouchAndDpad()

        binding.btnLogin.setOnClickListener {
            val user = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                realizarLoginMultiServidor(user, pass)
            }
        }
    }

    private fun realizarLoginMultiServidor(user: String, pass: String) {
        // Mostra o carregamento e trava o botão para garantir a sincronização 100%
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            var success = false
            var lastError: String? = null

            for (server in SERVERS) {
                val base = if (server.endsWith("/")) server.dropLast(1) else server
                
                // O Repository agora faz o Login E a Sincronização completa antes de retornar
                // Isso garante que a Home já abra cheia de conteúdo
                val result = repository.performLoginAndSync(base, user, pass)

                if (result.isSuccess) {
                    val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("dns", base)
                        putString("username", user)
                        putString("password", pass)
                        apply()
                    }
                    success = true
                    break
                } else {
                    lastError = result.exceptionOrNull()?.message ?: "Erro no servidor $base"
                }
            }

            if (success) {
                // Só pula para a Home após o Banco de Dados estar totalmente populado
                startHomeActivity()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Erro de Login em todos os servidores.\n$lastError",
                    Toast.LENGTH_LONG
                ).show()
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun startHomeActivity() {
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val savedDns = prefs.getString("dns", null)
        if (!savedDns.isNullOrBlank()) {
            XtreamApi.setBaseUrl(if (savedDns.endsWith("/")) savedDns else "$savedDns/")
        }
        
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    // ✅ D-Pad TV PREMIUM (COM ZOOM SUAVE) ✅ (MANTIDO INTEGRALMENTE)
    private fun setupTouchAndDpad() {
        val premiumFocusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
                v.isSelected = true
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                v.isSelected = false
            }
        }

        binding.etUsername.onFocusChangeListener = premiumFocusListener
        binding.etPassword.onFocusChangeListener = premiumFocusListener
        binding.btnLogin.onFocusChangeListener = premiumFocusListener
        
        binding.btnLogin.isFocusable = true
        binding.btnLogin.isFocusableInTouchMode = true
        
        binding.etUsername.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                binding.etPassword.requestFocus()
                true
            } else false
        }
        binding.etPassword.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                binding.btnLogin.performClick()
                true
            } else false
        }
        
        binding.etUsername.requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
