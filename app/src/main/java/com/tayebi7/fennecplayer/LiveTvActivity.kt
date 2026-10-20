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
import com.tayebi7.fennecplayer.data.Catalog
import com.tayebi7.fennecplayer.data.Category
import com.tayebi7.fennecplayer.data.ContentType
import com.tayebi7.fennecplayer.data.Entry
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.player.FennecPlayerManager
import com.tayebi7.fennecplayer.ui.CategoryAdapter
import com.tayebi7.fennecplayer.ui.ChannelAdapter
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
    private val channelAdapter = ChannelAdapter { play(it) }

    private var catalog: Catalog = Catalog.EMPTY
    private var currentCategory: Category? = null
    private var visibleChannels: List<Entry> = emptyList()
    private var current: Entry? = null
    private var query = ""

    private var player: ExoPlayer? = null
    private var live: FennecPlayerManager.LiveFallback? = null
    private var fullscreen = false

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = setFullscreen(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tv)
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
        live = FennecPlayerManager.LiveFallback(p) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@LiveTvActivity, "فشل تحميل القنوات: ${e.message}", Toast.LENGTH_LONG).show()
            }
            progress.visibility = View.GONE

            val all = Category("*", "كل القنوات", catalog.entries.size)
            val cats = listOf(all) + catalog.categories.filter { it.count > 0 }
            categoryAdapter.submit(cats)
            selectCategory(all, autoPlay = true)
        }
    }

    private fun selectCategory(category: Category, autoPlay: Boolean = false) {
        currentCategory = category
        tvCategoryTitle.text = category.name
        applyFilter()
        if (autoPlay && current == null) visibleChannels.firstOrNull()?.let { play(it) }
    }

    private fun applyFilter() {
        val cat = currentCategory ?: return
        val q = query
        visibleChannels = catalog.entries.filter {
            (cat.id == "*" || it.categoryId == cat.id) &&
                (q.isEmpty() || it.name.contains(q, ignoreCase = true))
        }
        channelAdapter.submit(visibleChannels)
        tvEmpty.visibility = if (visibleChannels.isEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = if (catalog.entries.isEmpty()) "لا توجد قنوات" else "لا توجد نتائج"
    }

    private fun play(entry: Entry) {
        val url = entry.url ?: return
        current = entry
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
