package com.tayebi7.fennecplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tayebi7.fennecplayer.data.AppSettings
import com.tayebi7.fennecplayer.data.Resume
import com.tayebi7.fennecplayer.player.FennecPlayerManager

/** مشغل ملء الشاشة للأفلام والحلقات، مع الانتقال التلقائي للعنصر التالي. */
class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var tvTitle: TextView
    private var player: ExoPlayer? = null

    private var titles: List<String> = emptyList()
    private var urls: List<String> = emptyList()
    private var index = 0
    private var position = 0L
    private var aspect = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.playerView)
        tvTitle = findViewById(R.id.tvTitle)

        titles = intent.getStringArrayListExtra(EXTRA_TITLES).orEmpty()
        urls = intent.getStringArrayListExtra(EXTRA_URLS).orEmpty()
        index = savedInstanceState?.getInt("index") ?: intent.getIntExtra(EXTRA_INDEX, 0)
        position = savedInstanceState?.getLong("pos") ?: 0L

        if (urls.isEmpty()) {
            finish()
            return
        }
        hideSystemBars()

        aspect = AppSettings.aspect(this)
        applyAspect()
        findViewById<View>(R.id.btnAspect).setOnClickListener {
            aspect = (aspect + 1) % 3
            applyAspect()
        }
    }

    private fun applyAspect() {
        playerView.resizeMode = when (aspect) {
            1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStart() {
        super.onStart()
        if (urls.isEmpty()) return
        val p = FennecPlayerManager.createPlayer(this)
        player = p
        playerView.player = p
        val start = index.coerceIn(0, urls.size - 1)
        // متابعة المشاهدة من آخر نقطة محفوظة
        val startPos = if (position > 0) position else Resume.position(this, urls[start])
        p.setMediaItems(urls.map { MediaItem.fromUri(it) }, start, startPos)
        p.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateTitle(p.currentMediaItemIndex)
            }

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@PlayerActivity, getString(R.string.play_error, error.errorCodeName), Toast.LENGTH_LONG).show()
            }
        })
        updateTitle(p.currentMediaItemIndex)
        p.prepare()
        p.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.let {
            index = it.currentMediaItemIndex
            position = it.currentPosition
            urls.getOrNull(index)?.let { u -> Resume.savePosition(this, u, position, it.duration.coerceAtLeast(0)) }
            it.release()
        }
        player = null
        playerView.player = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("index", index)
        outState.putLong("pos", position)
    }

    private fun updateTitle(i: Int) {
        tvTitle.text = titles.getOrNull(i).orEmpty()
    }

    companion object {
        private const val EXTRA_TITLES = "titles"
        private const val EXTRA_URLS = "urls"
        private const val EXTRA_INDEX = "index"

        fun start(context: Context, titles: ArrayList<String>, urls: ArrayList<String>, index: Int) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putStringArrayListExtra(EXTRA_TITLES, titles)
                    .putStringArrayListExtra(EXTRA_URLS, urls)
                    .putExtra(EXTRA_INDEX, index)
            )
        }
    }
}
