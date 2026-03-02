package com.vltv.play

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vltv.play.data.AppDatabase // ✅ Importação da Database
import com.vltv.play.data.StreamDao   // ✅ Importação do DAO
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class SearchActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var etQuery: EditText
    private lateinit var btnDoSearch: ImageButton
    private lateinit var rvResults: RecyclerView
    private lateinit var adapter: SearchResultAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    
    // ✅ Injeção da Database
    private lateinit var streamDao: StreamDao

    // Variáveis da Busca Otimizada
    private val supervisor = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + supervisor

    // LISTA MESTRA: Guarda tudo na memória para busca instantânea
    private var catalogoCompleto: List<SearchResultItem> = emptyList()
    private var isCarregandoDados = false
    private var jobBuscaInstantanea: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // ✅ Inicializa o banco de dados
        val database = AppDatabase.getDatabase(this)
        streamDao = database.streamDao()

        // Configuração de Tela Cheia / Barras
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        initViews()
        setupRecyclerView()
        setupSearchLogic()
        
        // ✅ O PULO DO GATO: Agora carrega do BANCO LOCAL (Muito mais rápido)
        carregarDadosDoBanco()
    }

    private fun initViews() {
        etQuery = findViewById(R.id.etQuery)
        btnDoSearch = findViewById(R.id.btnDoSearch)
        rvResults = findViewById(R.id.rvResults)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        // AJUSTE PARA O TECLADO NÃO COBRIR A TELA
        etQuery.imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter { item ->
            abrirDetalhes(item)
        }

        // 5 Colunas (Grade)
        rvResults.layoutManager = GridLayoutManager(this, 5)
        rvResults.adapter = adapter
        rvResults.isFocusable = true
        rvResults.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    private fun setupSearchLogic() {
        // TextWatcher: Detecta cada letra digitada
        etQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString().trim()
                
                // Se ainda está baixando os dados, avisa ou espera
                if (isCarregandoDados) return 

                // Cancela busca anterior e inicia nova (Instantânea)
                jobBuscaInstantanea?.cancel()
                jobBuscaInstantanea = launch {
                    delay(100) // Pequeno delay de 0.1s só para não piscar demais
                    filtrarNaMemoria(texto)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Botão de busca (teclado ou icone) força o filtro
        btnDoSearch.setOnClickListener { 
            filtrarNaMemoria(etQuery.text.toString().trim()) 
        }

        etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filtrarNaMemoria(etQuery.text.toString().trim())
                true
            } else false
        }
    }

    // ✅ NOVA LÓGICA: CARREGA DO BANCO (Substitui o carregarDadosIniciais antigo)
    private fun carregarDadosDoBanco() {
        isCarregandoDados = true
        progressBar.visibility = View.VISIBLE
        tvEmpty.text = "Acessando catálogo local..."
        tvEmpty.visibility = View.VISIBLE
        etQuery.isEnabled = false 

        launch {
            try {
                // ✅ Busca tudo no disco (Banco de Dados) ao mesmo tempo
                val resultados = withContext(Dispatchers.IO) {
                    val deferredFilmes = async { streamDao.getAllVods() }
                    val deferredSeries = async { streamDao.getAllSeries() }
                    val deferredCanais = async { streamDao.getAllLiveStreams() }

                    val filmes = deferredFilmes.await().map {
                        SearchResultItem(it.stream_id, it.name ?: "Sem Título", "movie", it.rating, it.stream_icon)
                    }
                    val series = deferredSeries.await().map {
                        SearchResultItem(it.series_id, it.name ?: "Sem Título", "series", it.rating, it.cover)
                    }
                    val canais = deferredCanais.await().map {
                        SearchResultItem(it.stream_id, it.name ?: "Sem Nome", "live", null, it.stream_icon)
                    }
                    
                    filmes + series + canais
                }

                catalogoCompleto = resultados
                isCarregandoDados = false
                
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.GONE
                etQuery.isEnabled = true
                etQuery.requestFocus()
                
                // Se veio algum texto da tela anterior (como da Área Kids), já filtra
                val initial = intent.getStringExtra("initial_query") ?: intent.getStringExtra("query") ?: intent.getStringExtra("search_text")
                if (!initial.isNullOrBlank()) {
                    etQuery.setText(initial)
                    filtrarNaMemoria(initial)
                } else {
                    tvEmpty.text = "Digite para buscar..."
                    tvEmpty.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                isCarregandoDados = false
                progressBar.visibility = View.GONE
                tvEmpty.text = "Erro ao acessar banco de dados."
                tvEmpty.visibility = View.VISIBLE
                etQuery.isEnabled = true
            }
        }
    }

    // --- LÓGICA DE FILTRO (INSTANTÂNEA NA MEMÓRIA) ---
    private fun filtrarNaMemoria(query: String) {
        if (catalogoCompleto.isEmpty()) return

        if (query.length < 1) {
            adapter.submitList(emptyList())
            tvEmpty.text = "Digite para buscar..."
            tvEmpty.visibility = View.VISIBLE
            return
        }

        val qNorm = query.lowercase().trim()

        // Filtragem Super Rápida na CPU
        val resultadosFiltrados = catalogoCompleto.filter { item ->
            item.title.lowercase().contains(qNorm)
        }.take(150) // ✅ Aumentado para 150 resultados

        adapter.submitList(resultadosFiltrados)
        
        if (resultadosFiltrados.isEmpty()) {
            tvEmpty.text = "Nenhum resultado encontrado."
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    // --- NAVEGAÇÃO ---
    private fun abrirDetalhes(item: SearchResultItem) {
        when (item.type) {
            "movie" -> {
                val i = Intent(this, DetailsActivity::class.java)
                i.putExtra("stream_id", item.id)
                i.putExtra("name", item.title)
                i.putExtra("icon", item.iconUrl ?: "")
                i.putExtra("rating", item.extraInfo ?: "0.0")
                startActivity(i)
            }
            "series" -> {
                val i = Intent(this, SeriesDetailsActivity::class.java)
                i.putExtra("series_id", item.id)
                i.putExtra("name", item.title)
                i.putExtra("icon", item.iconUrl ?: "")
                i.putExtra("rating", item.extraInfo ?: "0.0")
                startActivity(i)
            }
            "live" -> {
                val i = Intent(this, PlayerActivity::class.java)
                i.putExtra("stream_id", item.id)
                i.putExtra("name", item.title)
                i.putExtra("title", item.title)
                i.putExtra("type", "live")
                i.putExtra("epg_channel_id", "") 
                startActivity(i)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supervisor.cancel()
    }
}
