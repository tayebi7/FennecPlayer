
package com.fennecplayer.iptv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : Activity() {

    companion object {
        var playlistUrl: String = ""
        var playlistContent: String = ""
        var channelNames: List<String> = emptyList()
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(16, 24, 39))
        }

        val header = TextView(this).apply {
            text = "🦊 FENNEC PLAYER"
            textSize = 22f
            setTextColor(Color.rgb(255, 140, 50))
            setPadding(16, 18, 16, 18)
        }
        root.addView(header)

        playerView = PlayerView(this)
        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        val firstStream = findFirstStream(playlistContent)

        if (firstStream == null) {
            Toast.makeText(
                this,
                "No playable stream found in playlist",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            exoPlayer.setMediaItem(MediaItem.fromUri(firstStream))
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Toast.makeText(
                        this@PlayerActivity,
                        "Playback error: ${error.errorCodeName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    private fun findFirstStream(content: String): String? {
        val lines = content.lines()
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("#EXTINF:")) {
                for (j in i + 1 until lines.size) {
                    val line = lines[j].trim()
                    if (line.isNotEmpty() && !line.startsWith("#")) {
                        if (line.startsWith("http://") ||
                            line.startsWith("https://")) {
                            return line
                        }
                        break
                    }
                }
            }
        }
        return null
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
