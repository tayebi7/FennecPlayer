package com.tayebi7.fennecplayer.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

object FennecPlayerManager {
    private var exoPlayer: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    fun initializePlayer(
        context: Context,
        playerView: PlayerView,
        streamUrl: String,
        onError: (String) -> Unit
    ) {
        releasePlayer()

        // ضبط User-Agent لتجنب حظر السيرفرات تماماً مثل IBO Player Pro
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("IBOPlayerPro/1.0 (Linux; Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        // ضبط الـ Buffer لتسريع فتح القنوات ومنع التقطيع أثناء البث
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // Min buffer
                50000, // Max buffer
                1500,  // Buffer for playback (يفتح القناة بسرعة خلال ثانية ونصف)
                3000   // Buffer after rebuffer
            )
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .also { player ->
                playerView.player = player
                val mediaItem = MediaItem.fromUri(streamUrl)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true

                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        // في حال فشل صيغة .ts يقوم بتجربة .m3u8 تلقائياً كبديل
                        if (streamUrl.endsWith(".ts")) {
                            val fallbackUrl = streamUrl.replace(".ts", ".m3u8")
                            val fallbackItem = MediaItem.fromUri(fallbackUrl)
                            player.setMediaItem(fallbackItem)
                            player.prepare()
                            player.play()
                        } else {
                            onError("خطأ في تشغيل البث: ${error.localizedMessage}")
                        }
                    }
                })
            }
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
