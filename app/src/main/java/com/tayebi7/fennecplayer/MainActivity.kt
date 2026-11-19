package com.tayebi7.fennecplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tayebi7.fennecplayer.data.ContentType
import com.tayebi7.fennecplayer.data.PlaylistStore
import com.tayebi7.fennecplayer.data.PlaylistType
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** الشاشة الرئيسية: بطاقة البث + أفلام + مسلسلات + مفضلة + قوائم، وأزرار جانبية. */
class MainActivity : AppCompatActivity() {

    private lateinit var tvActivePlaylist: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLiveCount: TextView
    private lateinit var tvMovieCount: TextView
    private lateinit var tvSeriesCount: TextView
    private lateinit var progress: ProgressBar

    private var loadJob: Job? = null
    private var launchedAdd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()

        tvActivePlaylist = findViewById(R.id.tvActivePlaylist)
        tvStatus = findViewById(R.id.tvStatus)
        tvLiveCount = findViewById(R.id.tvLiveCount)
        tvMovieCount = findViewById(R.id.tvMovieCount)
        tvSeriesCount = findViewById(R.id.tvSeriesCount)
        progress = findViewById(R.id.progress)

        setupClicks()
        findViewById<View>(R.id.cardLiveTv).requestFocus()
    }

    override fun onResume() {
        super.onResume()
        loadPlaylist(force = false)
    }

    private fun setupClicks() {
        val live = findViewById<View>(R.id.cardLiveTv)
        val movies = findViewById<View>(R.id.cardMovies)
        val series = findViewById<View>(R.id.cardSeries)
        val fav = findViewById<View>(R.id.cardFavorites)
        val playlists = findViewById<View>(R.id.cardPlaylists)

        live.setOnClickListener { startActivity(Intent(this, LiveTvActivity::class.java)) }
        movies.setOnClickListener { BrowseActivity.start(this, ContentType.MOVIE) }
        series.setOnClickListener { BrowseActivity.start(this, ContentType.SERIES) }
        fav.setOnClickListener { showFavoritesPicker() }
        playlists.setOnClickListener { startActivity(Intent(this, PlaylistsActivity::class.java)) }

        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.btnReloadData).setOnClickListener { loadPlaylist(force = true) }
        findViewById<View>(R.id.btnFile).setOnClickListener {
            startActivity(
                Intent(this, ActivationActivity::class.java)
                    .putExtra(ActivationActivity.EXTRA_TAB, ActivationActivity.TAB_FILE)
            )
        }

        // تكبير خفيف للبطاقة المحددة بالريموت
        listOf(live, movies, series, fav, playlists).forEach { c ->
            c.setOnFocusChangeListener { v, focused ->
                val s = if (focused) 1.05f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            }
        }
    }

    private fun showFavoritesPicker() {
        val labels = arrayOf(
            getString(R.string.home_live), getString(R.string.home_movies), getString(R.string.home_series)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.fav_pick)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> startActivity(
                        Intent(this, LiveTvActivity::class.java).putExtra(LiveTvActivity.EXTRA_FAV, true)
                    )
                    1 -> BrowseActivity.start(this, ContentType.MOVIE, fav = true)
                    else -> BrowseActivity.start(this, ContentType.SERIES, fav = true)
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun loadPlaylist(force: Boolean) {
        val playlist = PlaylistStore.load(this)
        if (playlist == null) {
            // نفتح شاشة الإضافة مرة واحدة فقط لتفادي حلقة لا نهائية عند الضغط على رجوع
            if (!launchedAdd) {
                launchedAdd = true
                startActivity(Intent(this, ActivationActivity::class.java))
            }
            return
        }
        launchedAdd = false
        tvActivePlaylist.text = getString(R.string.status_playlist, playlist.name)

        if (force) Repo.invalidate()

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            tvStatus.text = getString(R.string.loading)
            tvLiveCount.text = "…"; tvMovieCount.text = "…"; tvSeriesCount.text = "…"

            coroutineScope {
                val live = async { runCatching { Repo.catalog(this@MainActivity, ContentType.LIVE) } }
                val movies = async { runCatching { Repo.catalog(this@MainActivity, ContentType.MOVIE) } }
                val series = async { runCatching { Repo.catalog(this@MainActivity, ContentType.SERIES) } }

                val l = live.await(); val m = movies.await(); val s = series.await()
                val failed = getString(R.string.failed_load)
                tvLiveCount.text = l.fold({ getString(R.string.count_channels, it.entries.size) }, { failed })
                tvMovieCount.text = m.fold({ getString(R.string.count_movies, it.entries.size) }, { failed })
                tvSeriesCount.text = s.fold({ getString(R.string.count_series, it.entries.size) }, { failed })

                tvStatus.text = when {
                    l.isFailure && m.isFailure && s.isFailure -> getString(R.string.status_failed)
                    playlist.type == PlaylistType.M3U -> getString(R.string.status_m3u)
                    playlist.type == PlaylistType.LOCAL -> getString(R.string.status_local)
                    else -> getString(R.string.status_xtream)
                }
            }
            progress.visibility = View.INVISIBLE
        }
    }
}
