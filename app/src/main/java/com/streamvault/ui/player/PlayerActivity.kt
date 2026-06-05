package com.streamvault.ui.player

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.streamvault.R
import com.streamvault.data.model.Channel
import com.streamvault.data.repository.MainRepository
import kotlinx.coroutines.*

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var tvTitle: TextView
    private lateinit var tvGroup: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnChannelList: ImageButton
    private lateinit var btnQuality: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var btnRetry: TextView
    private lateinit var topBar: View
    private lateinit var sidePanel: LinearLayout
    private lateinit var rvSideList: RecyclerView
    private lateinit var tvSidePanelTitle: TextView

    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private lateinit var channel: Channel
    private var allChannels = listOf<Channel>()
    private var isPanelOpen = false
    private var controlsVisible = true
    private val hideHandler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private val maxRetries = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pantalla completa inmersiva
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_player)
        channel = intent.getParcelableExtra("channel")!!
        bindViews()
        setupControls()
        loadChannelList()
        initPlayer(channel.url)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }

    private fun bindViews() {
        playerView    = findViewById(R.id.playerView)
        tvTitle       = findViewById(R.id.tvTitle)
        tvGroup       = findViewById(R.id.tvGroup)
        btnBack       = findViewById(R.id.btnBack)
        btnChannelList= findViewById(R.id.btnChannelList)
        btnQuality    = findViewById(R.id.btnQuality)
        progressBar   = findViewById(R.id.progressLoading)
        tvError       = findViewById(R.id.tvError)
        btnRetry      = findViewById(R.id.btnRetry)
        topBar        = findViewById(R.id.topBar)
        sidePanel     = findViewById(R.id.sidePanel)
        rvSideList    = findViewById(R.id.rvSideList)
        tvSidePanelTitle = findViewById(R.id.tvSidePanelTitle)
        tvTitle.text  = channel.name
        tvGroup.text  = channel.group ?: ""
    }

    private fun setupControls() {
        btnBack.setOnClickListener { finish() }
        btnQuality.setOnClickListener { showQualityMenu() }
        btnChannelList.setOnClickListener {
            if (isPanelOpen) closePanel() else openPanel()
        }
        btnRetry.setOnClickListener { retryCount = 0; initPlayer(channel.url) }

        // Tap en pantalla → mostrar/ocultar controles
        playerView.setOnClickListener {
            if (isPanelOpen) { closePanel(); return@setOnClickListener }
            if (controlsVisible) hideControls() else showControls()
        }
        // Auto-ocultar al arrancar
        scheduleHideControls()
    }

    private fun showControls() {
        controlsVisible = true
        topBar.animate().alpha(1f).setDuration(200).start()
        topBar.visibility = View.VISIBLE
        hideSystemUI()
        scheduleHideControls()
    }

    private fun hideControls() {
        controlsVisible = false
        topBar.animate().alpha(0f).setDuration(300).withEndAction {
            topBar.visibility = View.INVISIBLE
        }.start()
    }

    private fun scheduleHideControls() {
        hideHandler.removeCallbacksAndMessages(null)
        hideHandler.postDelayed({ if (!isPanelOpen) hideControls() }, 4000)
    }

    private fun loadChannelList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = getSharedPreferences("streamvault", Context.MODE_PRIVATE)
                val repo = MainRepository(prefs)
                val channels = mutableListOf<Channel>()
                repo.getSources().forEach { src ->
                    try { channels.addAll(repo.loadChannels(src)) } catch (_: Exception) {}
                }
                allChannels = channels
                withContext(Dispatchers.Main) { setupSidePanel() }
            } catch (_: Exception) {}
        }
    }

    private fun setupSidePanel() {
        val group = channel.group
        val list = if (!group.isNullOrEmpty())
            allChannels.filter { it.group.equals(group, ignoreCase = true) }
        else allChannels.take(200)

        tvSidePanelTitle.text = group?.uppercase() ?: "TODOS LOS CANALES"
        rvSideList.layoutManager = LinearLayoutManager(this)
        rvSideList.adapter = SidePanelAdapter(list, channel.id) { sel ->
            channel = sel
            tvTitle.text = sel.name
            tvGroup.text = sel.group ?: ""
            retryCount = 0
            initPlayer(sel.url)
            closePanel()
            // Actualizar panel con nuevo contexto de grupo
            CoroutineScope(Dispatchers.Main).launch {
                delay(300)
                setupSidePanel()
            }
        }
        // Scroll al canal actual
        val idx = list.indexOfFirst { it.id == channel.id }
        if (idx >= 0) rvSideList.scrollToPosition(idx)
    }

    private fun openPanel() {
        isPanelOpen = true
        showControls()
        hideHandler.removeCallbacksAndMessages(null)
        sidePanel.visibility = View.VISIBLE
        sidePanel.translationX = sidePanel.width.toFloat().coerceAtLeast(300f)
        sidePanel.animate().translationX(0f).setDuration(280).start()
    }

    private fun closePanel() {
        isPanelOpen = false
        sidePanel.animate().translationX(sidePanel.width.toFloat().coerceAtLeast(300f))
            .setDuration(220).withEndAction { sidePanel.visibility = View.GONE }.start()
        scheduleHideControls()
    }

    private fun showQualityMenu() {
        showControls()
        val ts = trackSelector ?: return
        val mappedInfo = ts.currentMappedTrackInfo

        // Construir opciones de calidad reales si existen
        val qualityOptions = mutableListOf<String>()
        val qualityValues = mutableListOf<Pair<Int,Int>>() // width, height

        qualityOptions.add("🔄 Auto (recomendado)")
        qualityValues.add(Pair(0, 0))

        if (mappedInfo != null) {
            for (ri in 0 until mappedInfo.rendererCount) {
                val trackGroups = mappedInfo.getTrackGroups(ri)
                for (gi in 0 until trackGroups.length) {
                    val group = trackGroups[gi]
                    for (ti in 0 until group.length) {
                        val fmt = group.getFormat(ti)
                        if (fmt.height > 0 && fmt.width > 0) {
                            val label = "${fmt.height}p${if (fmt.frameRate > 0) " ${fmt.frameRate.toInt()}fps" else ""}"
                            if (!qualityOptions.contains("📺 $label")) {
                                qualityOptions.add("📺 $label")
                                qualityValues.add(Pair(fmt.width, fmt.height))
                            }
                        }
                    }
                }
            }
        }
        // Opciones manuales siempre disponibles
        if (qualityOptions.size <= 1) {
            listOf(Pair("📺 1080p HD", Pair(1920,1080)), Pair("📺 720p HD", Pair(1280,720)),
                   Pair("📺 480p", Pair(854,480)), Pair("📺 360p", Pair(640,360)),
                   Pair("🎵 Solo audio", Pair(1,1))).forEach { (l, v) ->
                qualityOptions.add(l); qualityValues.add(v)
            }
        }

        AlertDialog.Builder(this, R.style.DarkDialog)
            .setTitle("Calidad de reproducción")
            .setItems(qualityOptions.toTypedArray()) { _: DialogInterface, which: Int ->
                val (w, h) = qualityValues[which]
                val p = ts.buildUponParameters()
                if (w == 0) p.clearVideoSizeConstraints()
                else if (w == 1) p.setMaxVideoSize(0, 0)
                else p.setMaxVideoSize(w, h)
                ts.setParameters(p)
                Toast.makeText(this, qualityOptions[which], Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun initPlayer(url: String) {
        player?.release()
        tvError.visibility = View.GONE
        btnRetry.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 60_000, 1_500, 3_000)
            .build()

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector!!)
            .setLoadControl(loadControl)
            .build().also { p ->
                playerView.player = p
                p.setMediaItem(MediaItem.Builder().setUri(url).build())
                p.prepare()
                p.playWhenReady = true
                p.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        progressBar.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                        if (state == Player.STATE_READY) {
                            tvError.visibility = View.GONE; btnRetry.visibility = View.GONE
                            retryCount = 0
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        if (retryCount < maxRetries) {
                            retryCount++
                            tvError.text = "⟳ Reconectando... ($retryCount/$maxRetries)"
                            tvError.visibility = View.VISIBLE
                            Handler(Looper.getMainLooper()).postDelayed({ initPlayer(url) }, 3000)
                        } else {
                            progressBar.visibility = View.GONE
                            tvError.text = "No se puede reproducir.\n${error.message}"
                            tvError.visibility = View.VISIBLE
                            btnRetry.visibility = View.VISIBLE
                        }
                    }
                })
            }
    }

    override fun onPause()   { super.onPause(); player?.pause() }
    override fun onResume()  { super.onResume(); player?.play(); hideSystemUI() }
    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacksAndMessages(null)
        player?.release(); player = null
    }
    override fun onBackPressed() {
        if (isPanelOpen) closePanel() else super.onBackPressed()
    }
}

class SidePanelAdapter(
    private val list: List<Channel>,
    private val currentId: String,
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<SidePanelAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val logo: ImageView = v.findViewById(R.id.imgLogo)
        val name: TextView  = v.findViewById(R.id.tvName)
        val group: TextView = v.findViewById(R.id.tvGroup)
        val bar: View       = v.findViewById(R.id.viewPlaying)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_side_channel, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val ch = list[pos]
        h.name.text  = ch.name
        h.group.text = ch.group ?: ""
        h.bar.visibility = if (ch.id == currentId) View.VISIBLE else View.INVISIBLE
        h.itemView.setBackgroundColor(if (ch.id == currentId) 0xFF0D1A2E.toInt() else 0xFF080812.toInt())
        Glide.with(h.logo).load(ch.logo).placeholder(com.streamvault.R.drawable.ic_channel).into(h.logo)
        h.itemView.setOnClickListener { onClick(ch) }
    }
}
