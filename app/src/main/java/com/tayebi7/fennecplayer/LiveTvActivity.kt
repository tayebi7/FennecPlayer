package com.tayebi7.fennecplayer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding
import com.tayebi7.fennecplayer.data.Catalog
import com.tayebi7.fennecplayer.data.Category
import com.tayebi7.fennecplayer.data.ContentType
import com.tayebi7.fennecplayer.data.Entry
import com.tayebi7.fennecplayer.data.Favorites
import com.tayebi7.fennecplayer.data.PlaylistStore
import com.tayebi7.fennecplayer.data.Resume
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.player.FennecPlayerManager
import com.tayebi7.fennecplayer.ui.CategoryAdapter
import com.tayebi7.fennecplayer.ui.ChannelAdapter
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding
import kotlinx.coroutines.launch

class LiveTvActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var playerView: PlayerView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvCurrentChannelName: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var progress: ProgressBar
    private lateinit var btnFullscreen: ImageButton

    private val categoryAdapter = CategoryAdapter { selectCategory(it) }
    private val channelAdapter = ChannelAdapter({ play(it) }, { toggleFavorite(it) })

    private var catalog: Catalog = Catalog.EMPTY
    private var currentCategory: Category? = null
    private var visibleChannels: List<Entry> = emptyList()
    private var current: Entry? = null
    private var query = ""
    private var playlistId = ""
    private var favIds: Set<String> = emptySet()

    private var player: ExoPlayer? = null
    private var live: FennecPlayerManager.LiveFallback? = null
    private var fullscreen = false

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = setFullscreen(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tv)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()
        findViewById<View>(android.R.id.content).applySystemBarsPadding()
        playlistId = PlaylistStore.load(this)?.id.orEmpty()
        initViews()
        onBackPressedDispatcher.addCallback(this, backCallback)
        loadData()
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
        current?.let { play(it) }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initViews() {
        rvCategories = findViewById(R.id.rvCategories)
        rvChannels = findViewById(R.id.rvChannels)
        playerView = findViewById(R.id.playerView)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)
        tvCurrentChannelName = findViewById(R.id.tvCurrentChannelName)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)
        progress = findViewById(R.id.progress)
        btnFullscreen = findViewById(R.id.btnFullscreen)

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvChannels.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = categoryAdapter
        rvChannels.adapter = channelAdapter

        btnFullscreen.setOnClickListener { setFullscreen(!fullscreen) }
        playerView.setOnClickListener { setFullscreen(!fullscreen) }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim().orEmpty()
                applyFilter()
            }
        })
    }

    private fun initPlayer() {
        if (player != null) return
        val p = FennecPlayerManager.createPlayer(this)
        player = p
        playerView.player = p
        live = FennecPlayerManager.LiveFallback(p) { code ->
            Toast.makeText(this, getString(R.string.channel_error, code), Toast.LENGTH_SHORT).show()
        }
    }

    private fun releasePlayer() {
        live?.release()
        live = null
        playerView.player = null
        player?.release()
        player = null
    }

    private fun loadData() {
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            try {
                catalog = Repo.catalog(this@LiveTvActivity, ContentType.LIVE)
            } catch (e: Exception) {
                Toast.makeText(this@LiveTvActivity, getString(R.string.load_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
            progress.visibility = View.GONE
            favIds = Favorites.ids(this@LiveTvActivity, playlistId, ContentType.LIVE)
            channelAdapter.favIds = favIds

            val all = Category("*", getString(R.string.all_channels), catalog.entries.size)
            val fav = Category(FAV_ID, getString(R.string.fav_category), favIds.size)
            val cats = listOf(all, fav) + catalog.categories.filter { it.count > 0 }
            categoryAdapter.submit(cats)

            val startFav = intent.getBooleanExtra(EXTRA_FAV, false)
            if (startFav) {
                categoryAdapter.select(1)
                selectCategory(fav)
            } else {
                selectCategory(all)
                // نتابع من آخر قناة تم تشغيلها
                if (current == null) {
                    val last = Resume.lastChannel(this@LiveTvActivity, playlistId)
                    (catalog.entries.firstOrNull { it.id == last } ?: visibleChannels.firstOrNull())?.let { play(it) }
                }
            }
        }
    }

    private fun selectCategory(category: Category) {
        currentCategory = category
        tvCategoryTitle.text = category.name
        applyFilter()
    }

    private fun toggleFavorite(entry: Entry) {
        val now = Favorites.toggle(this, playlistId, ContentType.LIVE, entry.id)
        favIds = Favorites.ids(this, playlistId, ContentType.LIVE)
        channelAdapter.favIds = favIds
        Toast.makeText(this, if (now) R.string.fav_added else R.string.fav_removed, Toast.LENGTH_SHORT).show()
        if (currentCategory?.id == FAV_ID) applyFilter()
    }

    private fun applyFilter() {
        val cat = currentCategory ?: return
        val q = query
        visibleChannels = catalog.entries.filter {
            (cat.id == "*" || (cat.id == FAV_ID && it.id in favIds) || it.categoryId == cat.id) &&
                (q.isEmpty() || it.name.contains(q, ignoreCase = true))
        }
        channelAdapter.submit(visibleChannels)
        tvEmpty.visibility = if (visibleChannels.isEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = getString(
            when {
                catalog.entries.isEmpty() -> R.string.no_channels
                cat.id == FAV_ID && favIds.isEmpty() -> R.string.no_favorites
                else -> R.string.no_results
            }
        )
    }

    private fun play(entry: Entry) {
        val url = entry.url ?: return
        current = entry
        Resume.setLastChannel(this, playlistId, entry.id)
        channelAdapter.playingId = entry.id
        tvCurrentChannelName.text = entry.name
        initPlayer()
        live?.play(url)
    }

    private fun zap(delta: Int) {
        if (visibleChannels.isEmpty()) return
        val idx = visibleChannels.indexOfFirst { it.id == current?.id }
        val next = (if (idx < 0) 0 else idx + delta).mod(visibleChannels.size)
        play(visibleChannels[next])
    }

    private fun setFullscreen(enable: Boolean) {
        fullscreen = enable
        backCallback.isEnabled = enable
        val side = if (enable) View.GONE else View.VISIBLE
        findViewById<View>(R.id.topBar).visibility = side
        findViewById<View>(R.id.colCategories).visibility = side
        findViewById<View>(R.id.colChannels).visibility = side
        findViewById<View>(R.id.playerInfo).visibility = side
        findViewById<View>(R.id.body).setPadding(
            if (enable) 0 else 12.dp(), if (enable) 0 else 12.dp(),
            if (enable) 0 else 12.dp(), if (enable) 0 else 12.dp()
        )
        if (enable) playerView.requestFocus()
    }

    companion object {
        const val EXTRA_FAV = "fav"
        private const val FAV_ID = "__fav__"
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (fullscreen) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> { zap(-1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> { zap(1); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
