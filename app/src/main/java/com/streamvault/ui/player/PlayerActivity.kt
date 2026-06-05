package com.streamvault.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.app.AlertDialog
import android.widget.*
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
import com.streamvault.data.model.SavedSource
import com.streamvault.data.repository.MainRepository
import com.streamvault.util.M3UParser
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

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
    private lateinit var tvRetry: TextView
    private lateinit var sidePanel: LinearLayout
    private lateinit var rvSideList: RecyclerView
    private lateinit var tvSidePanelTitle: TextView

    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private lateinit var channel: Channel
    private var allChannels = listOf<Channel>()
    private var isPanelOpen = false
    private val hideHandler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private val maxRetries = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        setContentView(R.layout.activity_player)
        channel = intent.getParcelableExtra("channel")!!
        setupViews()
        loadChannelList()
        initPlayer(channel.url)
    }

    private fun setupViews() {
        playerView = findViewById(R.id.playerView)
        tvTitle = findViewById(R.id.tvTitle)
        tvGroup = findViewById(R.id.tvGroup)
        btnBack = findViewById(R.id.btnBack)
        btnChannelList = findViewById(R.id.btnChannelList)
        btnQuality = findViewById(R.id.btnQuality)
        progressBar = findViewById(R.id.progressLoading)
        tvError = findViewById(R.id.tvError)
        tvRetry = findViewById(R.id.tvRetry)
        sidePanel = findViewById(R.id.sidePanel)
        rvSideList = findViewById(R.id.rvSideList)
        tvSidePanelTitle = findViewById(R.id.tvSidePanelTitle)

        tvTitle.text = channel.name
        tvGroup.text = channel.group ?: ""

        btnBack.setOnClickListener { finish() }
        btnChannelList.setOnClickListener { togglePanel() }
        btnQuality.setOnClickListener { showQualityMenu() }
        tvRetry.setOnClickListener { retryCount = 0; initPlayer(channel.url) }

        // Tap para mostrar/ocultar controles
        playerView.setOnClickListener {
            if (isPanelOpen) closePanel() else autoHideControls()
        }

        // Cerrar panel tocando fuera
        findViewById<View>(R.id.playerContainer).setOnClickListener {
            if (isPanelOpen) closePanel()
        }
    }

    private fun loadChannelList() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = getSharedPreferences("streamvault", Context.MODE_PRIVATE)
            val repo = MainRepository(prefs)
            val srcs = repo.getSources()
            val channels = mutableListOf<Channel>()
            srcs.forEach { src ->
                try { channels.addAll(repo.loadChannels(src)) } catch (e: Exception) {}
            }
            allChannels = channels
            withContext(Dispatchers.Main) { setupSidePanel() }
        }
    }

    private fun setupSidePanel() {
        val group = channel.group
        val filtered = if (!group.isNullOrEmpty())
            allChannels.filter { it.group == group }
        else allChannels.take(100)

        tvSidePanelTitle.text = if (!group.isNullOrEmpty()) "📺 $group" else "📺 Todos los canales"
        rvSideList.layoutManager = LinearLayoutManager(this)
        rvSideList.adapter = SidePanelAdapter(filtered, channel.id) { selected ->
            channel = selected
            tvTitle.text = selected.name
            tvGroup.text = selected.group ?: ""
            retryCount = 0
            initPlayer(selected.url)
            closePanel()
        }
    }

    private fun togglePanel() {
        if (isPanelOpen) closePanel() else openPanel()
    }

    private fun openPanel() {
        sidePanel.visibility = View.VISIBLE
        sidePanel.animate().translationX(0f).setDuration(250).start()
        isPanelOpen = true
    }

    private fun closePanel() {
        sidePanel.animate().translationX(sidePanel.width.toFloat()).setDuration(200).withEndAction {
            sidePanel.visibility = View.GONE
        }.start()
        isPanelOpen = false
    }

    private fun autoHideControls() {
        hideHandler.removeCallbacksAndMessages(null)
        hideHandler.postDelayed({
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }, 3000)
    }

    private fun showQualityMenu() {
        val ts = trackSelector ?: return
        val params = ts.currentMappedTrackInfo ?: run {
            Toast.makeText(this, "Calidad no disponible", Toast.LENGTH_SHORT).show(); return
        }
        val items = arrayOf("Auto (recomendado)", "1080p", "720p", "480p", "360p", "Solo audio")
        AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setTitle("Calidad de video")
            .setItems(items) { _: android.content.DialogInterface, which: Int ->
                val override = ts.buildUponParameters()
                when (which) {
                    0 -> override.clearVideoSizeConstraints()
                    1 -> override.setMaxVideoSize(1920, 1080)
                    2 -> override.setMaxVideoSize(1280, 720)
                    3 -> override.setMaxVideoSize(854, 480)
                    4 -> override.setMaxVideoSize(640, 360)
                    5 -> override.setMaxVideoSize(0, 0)
                }
                ts.setParameters(override)
                Toast.makeText(this, "Calidad: ${items[which]}", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun initPlayer(url: String) {
        player?.release()
        tvError.visibility = View.GONE
        tvRetry.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 60_000, 2_500, 5_000)
            .build()

        trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector!!)
            .setLoadControl(loadControl)
            .build().also { p ->
                playerView.player = p
                val item = MediaItem.Builder()
                    .setUri(url)
                    .build()
                p.setMediaItem(item)
                p.prepare()
                p.playWhenReady = true

                p.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        progressBar.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                        if (state == Player.STATE_READY) {
                            tvError.visibility = View.GONE
                            tvRetry.visibility = View.GONE
                            retryCount = 0
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        if (retryCount < maxRetries) {
                            retryCount++
                            tvError.text = "Reconectando... ($retryCount/$maxRetries)"
                            tvError.visibility = View.VISIBLE
                            Handler(Looper.getMainLooper()).postDelayed({
                                initPlayer(url)
                            }, 3000)
                        } else {
                            progressBar.visibility = View.GONE
                            tvError.text = "No se puede reproducir este canal.\nVerifica tu conexion."
                            tvError.visibility = View.VISIBLE
                            tvRetry.visibility = View.VISIBLE
                        }
                    }
                })
            }
    }

    override fun onPause() { super.onPause(); player?.pause() }
    override fun onResume() { super.onResume(); player?.play() }
    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacksAndMessages(null)
        player?.release(); player = null
    }
}

// Adapter para el panel lateral
class SidePanelAdapter(
    private val list: List<Channel>,
    private val currentId: String,
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<SidePanelAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val logo: ImageView = v.findViewById(R.id.imgLogo)
        val name: TextView = v.findViewById(R.id.tvName)
        val group: TextView = v.findViewById(R.id.tvGroup)
        val playing: View = v.findViewById(R.id.viewPlaying)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_side_channel, p, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val ch = list[pos]
        h.name.text = ch.name
        h.group.text = ch.group ?: ""
        h.playing.visibility = if (ch.id == currentId) View.VISIBLE else View.INVISIBLE
        Glide.with(h.logo).load(ch.logo).placeholder(R.drawable.ic_channel).into(h.logo)
        h.itemView.setBackgroundColor(
            if (ch.id == currentId) 0xFF1A2A3A.toInt() else 0xFF0A0A0F.toInt()
        )
        h.itemView.setOnClickListener { onClick(ch) }
    }
}
