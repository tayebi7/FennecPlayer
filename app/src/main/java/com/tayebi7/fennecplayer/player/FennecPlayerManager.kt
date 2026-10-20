package com.tayebi7.fennecplayer.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.tayebi7.fennecplayer.network.XtreamApiService

object FennecPlayerManager {

    /** ينشئ مشغلاً جديداً. على كل شاشة أن تحرره بنفسها في onStop/onDestroy. */
    @OptIn(UnstableApi::class)
    fun createPlayer(context: Context): ExoPlayer {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(XtreamApiService.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val renderers = DefaultRenderersFactory(context.applicationContext)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15000, 50000, 1500, 3000)
            .build()

        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .setLoadControl(loadControl)
            .build()
    }

    /**
     * يشغل قناة مباشرة، وفي حال فشل رابط .ts يجرب .m3u8 مرة واحدة فقط
     * (النسخة القديمة كانت تدخل في حلقة إعادة لا نهائية عند فشل البديل).
     */
    class LiveFallback(
        private val player: ExoPlayer,
        private val onError: (String) -> Unit
    ) : Player.Listener {
        private var candidates: List<String> = emptyList()
        private var index = 0

        init {
            player.addListener(this)
        }

        fun play(url: String) {
            candidates = if (url.endsWith(".ts", ignoreCase = true)) {
                listOf(url, url.dropLast(3) + ".m3u8")
            } else listOf(url)
            index = 0
            load()
        }

        fun release() = player.removeListener(this)

        private fun load() {
            player.setMediaItem(MediaItem.fromUri(candidates[index]))
            player.prepare()
            player.playWhenReady = true
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition()
                player.prepare()
                return
            }
            if (index < candidates.size - 1) {
                index++
                load()
            } else {
                onError("تعذر تشغيل القناة (${error.errorCodeName})")
            }
        }
    }
}
