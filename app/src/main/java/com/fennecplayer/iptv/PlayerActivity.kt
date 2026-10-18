
package com.fennecplayer.iptv

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra("name") ?: "Fennec Player"
        val url = intent.getStringExtra("url").orEmpty()

        window.statusBarColor = Color.rgb(7, 17, 38)
        window.navigationBarColor = Color.BLACK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        val title = TextView(this).apply {
            text = name
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
        }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        }

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        if (url.isBlank()) {
            title.text = "$name\nNo stream URL was provided."
            return
        }

        try {
            player = ExoPlayer.Builder(this).build().also { exo ->
                playerView.player = exo

                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        title.text = "$name\nPlayback failed: ${error.errorCodeName}"
                    }
                })

                exo.setMediaItem(MediaItem.fromUri(url))
                exo.prepare()
                exo.playWhenReady = true
            }
        } catch (e: Exception) {
            title.text = "$name\nUnable to start playback."
        }
    }

    override fun onStop() {
        player?.release()
        player = null
        super.onStop()
    }
}
