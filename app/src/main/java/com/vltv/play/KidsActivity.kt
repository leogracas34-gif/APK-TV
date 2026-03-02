package com.vltv.play

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.vltv.play.data.AppDatabase // ✅ Importação da Database
import com.vltv.play.data.StreamDao   // ✅ Importação do DAO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KidsActivity : AppCompatActivity() {
    private lateinit var rvHubChannels: RecyclerView
    private lateinit var rvRecentKids: RecyclerView
    private lateinit var rvMoviesKids: RecyclerView
    private lateinit var rvSeriesKids: RecyclerView
    private lateinit var tvTitleRecent: TextView
    private lateinit var etSearchKids: EditText
    private lateinit var prefs: SharedPreferences
    private var user = ""
    private var pass = ""

    // ✅ Injeção da Database
    private lateinit var streamDao: StreamDao

    private val termosProibidos = listOf(
        "adulto", "xxx", "sexo", "sexy", "porn", "18+", "erótico", "violência", 
        "007", "terror", "horror", "assassinato", "guerra", "pânico", "morte"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())

        setContentView(R.layout.activity_kids)

        // ✅ Inicializa o banco de dados
        val database = AppDatabase.getDatabase(this)
        streamDao = database.streamDao()

        prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        user = prefs.getString("username", "") ?: ""
        pass = prefs.getString("password", "") ?: ""

        tvTitleRecent = findViewById(R.id.tvTitleRecent)
        rvHubChannels = findViewById(R.id.rvHubChannels)
        rvRecentKids = findViewById(R.id.rvRecentKids)
        rvMoviesKids = findViewById(R.id.rvMoviesKids)
        rvSeriesKids = findViewById(R.id.rvSeriesKids)
        etSearchKids = findViewById(R.id.etSearchKids)

        findViewById<TextView>(R.id.btnBackKids).let {
            configurarFoco(it)
            it.setOnClickListener { finish() }
        }

        configurarFoco(etSearchKids)
        etSearchKids.isFocusableInTouchMode = true 
        etSearchKids.setOnClickListener {
            etSearchKids.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearchKids, InputMethodManager.SHOW_FORCED)
        }

        etSearchKids.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                val query = v.text.toString().trim()
                val contemProibido = termosProibidos.any { query.contains(it, ignoreCase = true) }
                
                if (query.isNotEmpty() && !contemProibido) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    etSearchKids.clearFocus()
                    val intent = Intent(this, SearchActivity::class.java).apply {
                        putExtra("query", query)
                        putExtra("search_text", query)
                        putExtra("initial_query", query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } else if (contemProibido) {
                    Toast.makeText(this, "Busca bloqueada na Área Kids 🛡️", Toast.LENGTH_LONG).show()
                    etSearchKids.setText("")
                }
                true
            } else false
        }

        setupLayouts()
        setupHubChannels()
        carregarConteudoKids()
    }

    override fun onResume() {
        super.onResume()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        etSearchKids.setText("")
        etSearchKids.clearFocus()
        atualizarRecentesVisual()
    }

    private fun configurarFoco(view: View) {
        view.isFocusable = true
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                v.setBackgroundResource(R.drawable.bg_selector_kids)
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                v.background = null
            }
        }
    }

    private fun setupLayouts() {
        rvHubChannels.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rvRecentKids.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rvMoviesKids.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rvSeriesKids.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
    }

    private fun setupHubChannels() {
        val nomesDesejados = listOf("Cartoon Network", "Discovery Kids", "Gloob", "Cartoonito", "Nickelodeon")
        
        // ✅ ATUALIZADO: Busca instantânea no Banco de Dados
        CoroutineScope(Dispatchers.Main).launch {
            val todosCanaisDb = withContext(Dispatchers.IO) { streamDao.getAllLiveStreams() }
            
            if (todosCanaisDb.isNotEmpty()) {
                val listaHub = mutableListOf<LiveStream>()
                nomesDesejados.forEach { nomeBusca ->
                    todosCanaisDb.firstOrNull { it.name.contains(nomeBusca, ignoreCase = true) }?.let {
                        listaHub.add(LiveStream(it.stream_id, it.name, it.stream_icon, it.epg_channel_id))
                    }
                }
                rvHubChannels.adapter = HubAdapter(listaHub) { canal ->
                    val intent = Intent(this@KidsActivity, PlayerActivity::class.java).apply {
                        putExtra("stream_id", canal.id)
                        putExtra("name", canal.name)
                        putExtra("title", canal.name)
                        putExtra("type", "live")
                        putExtra("epg_channel_id", canal.epg_channel_id)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun carregarConteudoKids() {
        CoroutineScope(Dispatchers.Main).launch {
            // ✅ FILMES KIDS DO BANCO
            val todosVods = withContext(Dispatchers.IO) { streamDao.getAllVods() }
            val filmesKids = todosVods.filter {
                val n = it.name.lowercase()
                n.contains("kids") || n.contains("infantil") || n.contains("desenho") || n.contains("disney")
            }.map { VodStream(it.stream_id, it.name, it.title, it.stream_icon, it.container_extension, it.rating) }

            if (filmesKids.isNotEmpty()) {
                rvMoviesKids.adapter = KidsVodAdapter(filmesKids) { filme ->
                    salvarNosRecentes(filme.id.toString(), "movie")
                    abrirDetalhesFilme(filme)
                }
            }

            // ✅ SÉRIES KIDS DO BANCO
            val todasSeries = withContext(Dispatchers.IO) { streamDao.getAllSeries() }
            val seriesKids = todasSeries.filter {
                val n = it.name.lowercase()
                n.contains("kids") || n.contains("infantil") || n.contains("desenho") || n.contains("disney")
            }.map { SeriesStream(it.series_id, it.name, it.cover, it.rating) }

            if (seriesKids.isNotEmpty()) {
                rvSeriesKids.adapter = KidsSeriesAdapter(seriesKids) { serie ->
                    salvarNosRecentes(serie.id.toString(), "series")
                    abrirDetalhesSerie(serie)
                }
            }
        }
    }

    private fun salvarNosRecentes(id: String, tipo: String) {
        val key = if (tipo == "movie") "kids_recent_vod" else "kids_recent_series"
        val atuais = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        atuais.remove(id)
        atuais.add(id)
        prefs.edit().putStringSet(key, atuais.take(10).toSet()).apply()
    }

    private fun atualizarRecentesVisual() {
        val recentVodIds = prefs.getStringSet("kids_recent_vod", emptySet()) ?: emptySet()
        val recentSeriesIds = prefs.getStringSet("kids_recent_series", emptySet()) ?: emptySet()
        
        if (recentVodIds.isNotEmpty() || recentSeriesIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                val listaRecentesUnificada = mutableListOf<KidsRecentItem>()
                
                val todosVods = withContext(Dispatchers.IO) { streamDao.getAllVods() }
                todosVods.filter { recentVodIds.contains(it.stream_id.toString()) }.forEach {
                    val obj = VodStream(it.stream_id, it.name, it.title, it.stream_icon, it.container_extension, it.rating)
                    listaRecentesUnificada.add(KidsRecentItem(it.stream_id.toString(), it.name ?: "Filme", it.stream_icon ?: "", "movie", obj, null))
                }

                val todasSeries = withContext(Dispatchers.IO) { streamDao.getAllSeries() }
                todasSeries.filter { recentSeriesIds.contains(it.series_id.toString()) }.forEach {
                    val obj = SeriesStream(it.series_id, it.name, it.cover, it.rating)
                    listaRecentesUnificada.add(KidsRecentItem(it.series_id.toString(), it.name ?: "Série", it.cover ?: "", "series", null, obj))
                }
                
                exibirRecentes(listaRecentesUnificada)
            }
        }
    }

    private fun exibirRecentes(itens: List<KidsRecentItem>) {
        val listaFinal = itens.distinctBy { it.id }.reversed()
        if (listaFinal.isNotEmpty()) {
            tvTitleRecent.visibility = View.VISIBLE
            rvRecentKids.visibility = View.VISIBLE
            rvRecentKids.adapter = KidsUnifiedAdapter(listaFinal) { item ->
                if (item.tipo == "movie" && item.filmeObj != null) abrirDetalhesFilme(item.filmeObj)
                else if (item.tipo == "series" && item.serieObj != null) abrirDetalhesSerie(item.serieObj)
            }
        }
    }

    private fun abrirDetalhesFilme(filme: VodStream) {
        val intent = Intent(this, DetailsActivity::class.java).apply {
            putExtra("stream_id", filme.id)
            putExtra("stream_ext", filme.extension ?: "mp4")
            putExtra("name", filme.name)
            putExtra("icon", filme.icon)
            putExtra("rating", filme.rating ?: "0.0")
        }
        startActivity(intent)
    }

    private fun abrirDetalhesSerie(serie: SeriesStream) {
        val intent = Intent(this, SeriesDetailsActivity::class.java).apply {
            putExtra("series_id", serie.id)
            putExtra("name", serie.name)
            putExtra("icon", serie.icon)
        }
        startActivity(intent)
    }

    data class KidsRecentItem(val id: String, val nome: String, val capa: String, val tipo: String, val filmeObj: VodStream?, val serieObj: SeriesStream?)

    inner class HubAdapter(val list: List<LiveStream>, val onClick: (LiveStream) -> Unit) : RecyclerView.Adapter<HubAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgLogoHub)
            val txt: TextView = v.findViewById(R.id.tvNameHub)
            val container: LinearLayout = v.findViewById(R.id.containerHub)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = 
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_hub_kids, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            val nomeUpper = item.name.uppercase()
            holder.txt.text = nomeUpper

            Glide.with(holder.itemView.context)
                .load(item.icon)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter() 
                .into(holder.img)

            val corFundo = when {
                nomeUpper.contains("CARTOON") -> "#000000"
                nomeUpper.contains("DISCOVERY") -> "#00AEEF"
                nomeUpper.contains("NICK") -> "#FF6600"
                nomeUpper.contains("GLOOB") -> "#E30613"
                nomeUpper.contains("DISNEY") -> "#FF007F"
                else -> "#4A148C"
            }
            holder.container.setBackgroundColor(Color.parseColor(corFundo))

            configurarFoco(holder.itemView)
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }

    inner class KidsUnifiedAdapter(val list: List<KidsRecentItem>, val onClick: (KidsRecentItem) -> Unit) : RecyclerView.Adapter<KidsUnifiedAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgPoster)
            val txt: TextView = v.findViewById(R.id.tvName)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_vod, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.txt.text = item.nome
            Glide.with(holder.itemView.context).load(item.capa).diskCacheStrategy(DiskCacheStrategy.ALL).override(200, 300).centerCrop().into(holder.img)
            configurarFoco(holder.itemView)
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }

    inner class KidsVodAdapter(val list: List<VodStream>, val onClick: (VodStream) -> Unit) : RecyclerView.Adapter<KidsVodAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgPoster)
            val txt: TextView = v.findViewById(R.id.tvName)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_vod, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.txt.text = item.name
            Glide.with(holder.itemView.context).load(item.icon).diskCacheStrategy(DiskCacheStrategy.ALL).override(200, 300).centerCrop().into(holder.img)
            configurarFoco(holder.itemView)
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }

    inner class KidsSeriesAdapter(val list: List<SeriesStream>, val onClick: (SeriesStream) -> Unit) : RecyclerView.Adapter<KidsSeriesAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgPoster)
            val txt: TextView = v.findViewById(R.id.tvName)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_vod, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.txt.text = item.name
            Glide.with(holder.itemView.context).load(item.icon).diskCacheStrategy(DiskCacheStrategy.ALL).override(200, 300).centerCrop().into(holder.img)
            configurarFoco(holder.itemView)
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }
}
