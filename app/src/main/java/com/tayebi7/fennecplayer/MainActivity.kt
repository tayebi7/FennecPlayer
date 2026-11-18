package com.tayebi7.fennecplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding
import com.tayebi7.fennecplayer.data.ContentType
import com.tayebi7.fennecplayer.data.PlaylistStore
import com.tayebi7.fennecplayer.data.PlaylistType
import com.tayebi7.fennecplayer.data.Repo
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvActivePlaylist: TextView
    private lateinit var tvExpiration: TextView
    private lateinit var tvLiveCount: TextView
    private lateinit var tvMovieCount: TextView
    private lateinit var tvSeriesCount: TextView
    private lateinit var progress: ProgressBar

    private lateinit var cardLiveTv: LinearLayout
    private lateinit var cardMovies: LinearLayout
    private lateinit var cardSeries: LinearLayout

    private lateinit var btnChangePlaylist: Button
    private lateinit var btnReloadData: Button
    private lateinit var btnExitApp: Button

    private var loadJob: Job? = null
    private var launchedActivation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()
        initViews()
        setupListeners()
        cardLiveTv.requestFocus()
        // تكبير خفيف للبطاقة المحددة بالريموت (كأسلوب IBO)
        listOf(cardLiveTv, cardMovies, cardSeries).forEach { c ->
            c.setOnFocusChangeListener { v, focused ->
                v.animate().scaleX(if (focused) 1.06f else 1f).scaleY(if (focused) 1.06f else 1f).setDuration(120).start()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPlaylist(force = false)
    }

    private fun initViews() {
        tvActivePlaylist = findViewById(R.id.tvActivePlaylist)
        tvExpiration = findViewById(R.id.tvExpiration)
        tvLiveCount = findViewById(R.id.tvLiveCount)
        tvMovieCount = findViewById(R.id.tvMovieCount)
        tvSeriesCount = findViewById(R.id.tvSeriesCount)
        progress = findViewById(R.id.progress)

        cardLiveTv = findViewById(R.id.cardLiveTv)
        cardMovies = findViewById(R.id.cardMovies)
        cardSeries = findViewById(R.id.cardSeries)

        btnChangePlaylist = findViewById(R.id.btnChangePlaylist)
        btnReloadData = findViewById(R.id.btnReloadData)
        btnExitApp = findViewById(R.id.btnExitApp)
    }

    private fun loadPlaylist(force: Boolean) {
        val playlist = PlaylistStore.load(this)
        if (playlist == null) {
            // نفتح شاشة التفعيل مرة واحدة فقط لتفادي حلقة لا نهائية عند الضغط على رجوع
            if (!launchedActivation) {
                launchedActivation = true
                startActivity(Intent(this, ActivationActivity::class.java))
            }
            return
        }
        launchedActivation = false
        tvActivePlaylist.text = "القائمة: ${playlist.name}"

        if (force) Repo.invalidate()

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            tvExpiration.text = "جاري الاتصال..."
            tvLiveCount.text = "…"; tvMovieCount.text = "…"; tvSeriesCount.text = "…"

            coroutineScope {
                val live = async { runCatching { Repo.catalog(this@MainActivity, ContentType.LIVE) } }
                val movies = async { runCatching { Repo.catalog(this@MainActivity, ContentType.MOVIE) } }
                val series = async { runCatching { Repo.catalog(this@MainActivity, ContentType.SERIES) } }

                val l = live.await(); val m = movies.await(); val s = series.await()
                tvLiveCount.text = l.fold({ "${it.entries.size} قناة" }, { "تعذر التحميل" })
                tvMovieCount.text = m.fold({ "${it.entries.size} فيلم" }, { "تعذر التحميل" })
                tvSeriesCount.text = s.fold({ "${it.entries.size} مسلسل" }, { "تعذر التحميل" })

                val failedAll = l.isFailure && m.isFailure && s.isFailure
                tvExpiration.text = when {
                    failedAll -> "تعذر الاتصال بالسيرفر"
                    playlist.type == PlaylistType.M3U -> "قائمة M3U"
                    else -> "Xtream"
                }
            }
            progress.visibility = View.INVISIBLE
        }
    }

    private fun setupListeners() {
        cardLiveTv.setOnClickListener { startActivity(Intent(this, LiveTvActivity::class.java)) }
        cardMovies.setOnClickListener { BrowseActivity.start(this, ContentType.MOVIE) }
        cardSeries.setOnClickListener { BrowseActivity.start(this, ContentType.SERIES) }

        btnChangePlaylist.setOnClickListener {
            startActivity(Intent(this, ActivationActivity::class.java))
        }
        btnReloadData.setOnClickListener {
            Toast.makeText(this, "جاري تحديث البيانات...", Toast.LENGTH_SHORT).show()
            loadPlaylist(force = true)
        }
        btnExitApp.setOnClickListener { finishAffinity() }
    }
}
