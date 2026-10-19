
package com.fennecplayer.iptv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val orange = Color.rgb(255, 140, 50)
    private val backgroundColor = Color.rgb(16, 24, 39)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 40, 24, 24)
            setBackgroundColor(backgroundColor)
        }

        val logo = TextView(this).apply {
            text = "🦊"
            textSize = 64f
            gravity = Gravity.CENTER
        }
        root.addView(logo)

        val title = TextView(this).apply {
            text = "FENNEC PLAYER"
            textSize = 28f
            setTextColor(orange)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Your IPTV player"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle)

        val playlistUrl = EditText(this).apply {
            hint = "Enter M3U playlist URL"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
        }

        val urlParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        urlParams.topMargin = 32
        root.addView(playlistUrl, urlParams)

        val loadButton = Button(this).apply {
            text = "Load Playlist"
            setTextColor(Color.WHITE)
            setBackgroundColor(orange)
        }

        val buttonParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        buttonParams.topMargin = 16
        root.addView(loadButton, buttonParams)

        val status = TextView(this).apply {
            text = "Enter a playlist URL to begin."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val statusParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        statusParams.topMargin = 20
        root.addView(status, statusParams)

        loadButton.setOnClickListener {
            val address = playlistUrl.text.toString().trim()

            if (address.isBlank()) {
                playlistUrl.error = "Enter a playlist URL"
                return@setOnClickListener
            }

            if (!address.startsWith("https://") &&
                !address.startsWith("http://")) {
                playlistUrl.error = "URL must start with http:// or https://"
                return@setOnClickListener
            }

            status.text = "Loading playlist..."
            loadButton.isEnabled = false

            thread {
                try {
                    val connection = URL(address).openConnection()
                        as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.requestMethod = "GET"

                    val content = connection.inputStream.bufferedReader()
                        .use { it.readText() }
                    connection.disconnect()

                    val streams = content.lines().filter {
                        it.trim().startsWith("#EXTINF:")
                    }

                    runOnUiThread {
                        loadButton.isEnabled = true

                        if (streams.isEmpty()) {
                            status.text = "No channels found. Check your M3U URL."
                            Toast.makeText(
                                this,
                                "Playlist is empty or unsupported",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            status.text = "Found ${streams.size} channels."
                            PlayerActivity.playlistUrl = address
                            PlayerActivity.channelNames = streams.map {
                                it.substringAfterLast(",").trim()
                            }
                            PlayerActivity.playlistContent = content

                            startActivity(
                                android.content.Intent(
                                    this,
                                    PlayerActivity::class.java
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        loadButton.isEnabled = true
                        status.text = "Could not load playlist."
                        Toast.makeText(
                            this,
                            e.message ?: "Network error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        setContentView(root)
    }
}
