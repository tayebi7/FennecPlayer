
package com.fennecplayer.iptv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import org.json.JSONArray
import java.net.URL
import java.util.concurrent.Executors

private const val NAVY = 0xFF071126.toInt()
private const val PANEL = 0xFF101E38.toInt()
private const val ORANGE = 0xFFFF8A00.toInt()
private const val CYAN = 0xFF00CFFF.toInt()
private const val PREFS = "fennec_player"

private data class Channel(
    val name: String,
    val url: String,
    val group: String = "Live TV"
)

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var playlistInput: EditText
    private var channels = mutableListOf<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = NAVY
        window.navigationBarColor = NAVY

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NAVY)
            setPadding(18)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.fennec_logo)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(70), dp(70)))

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 0, 0, 0)
        }

        heading.addView(TextView(this).apply {
            text = "FENNEC PLAYER"
            textSize = 23f
            setTextColor(Color.WHITE)
            setTypeface(null, 1)
        })

        heading.addView(TextView(this).apply {
            text = "IPTV • YOUR STREAMS, YOUR WAY"
            textSize = 11f
            setTextColor(CYAN)
        })

        header.addView(heading)
        root.addView(header)

        root.addView(TextView(this).apply {
            text = "\nAdd your M3U playlist"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, 1)
        })

        playlistInput = EditText(this).apply {
            hint = "https://example.com/playlist.m3u"
            setHintTextColor(0xFF8B9AB5.toInt())
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
            setPadding(dp(12))
            setBackgroundColor(PANEL)
            setText(
                getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString("playlist_url", "") ?: ""
            )
        }

        root.addView(
            playlistInput,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(10)
            }
        )

        val load = Button(this).apply {
            text = "LOAD PLAYLIST"
            setTextColor(Color.WHITE)
            setBackgroundColor(ORANGE)
            setOnClickListener { loadPlaylist() }
        }

        root.addView(
            load,
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                topMargin = dp(10)
            }
        )

        status = TextView(this).apply {
            text = "Add a playlist URL to load your channels."
            textSize = 13f
            setTextColor(0xFFB8C7DF.toInt())
            setPadding(0, dp(12), 0, dp(8))
        }
        root.addView(status)

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(list)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        root.addView(TextView(this).apply {
            text = "© 2026 Fennec Player • Abdelghani Tayebi\n" +
                "All rights reserved. Use only streams you are authorized to access."
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(0xFF72819D.toInt())
            setPadding(0, dp(12), 0, dp(4))
        })

        setContentView(root)

        val saved = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString("channels_json", null)

        if (saved != null) {
            runCatching {
                val arr = JSONArray(saved)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    channels.add(
                        Channel(
                            o.getString("name"),
                            o.getString("url"),
                            o.optString("group", "Live TV")
                        )
                    )
                }
                renderChannels()
            }
        }
    }

    private fun loadPlaylist() {
        val address = playlistInput.text.toString().trim()

        if (!address.startsWith("https://") &&
            !address.startsWith("http://")) {
            status.text = "Enter a valid http(s) M3U playlist URL."
            return
        }

        status.text = "Loading playlist…"

        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString("playlist_url", address)
            .apply()

        executor.execute {
            try {
                val content = URL(address).openConnection().apply {
                    connectTimeout = 12000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "FennecPlayer/1.0")
                }.getInputStream().bufferedReader().use {
                    it.readText()
                }

                val parsed = parseM3u(content)

                runOnUiThread {
                    channels = parsed
                    saveChannels()
                    renderChannels()
                    status.text = if (channels.isEmpty()) {
                        "No playable entries found. Check your playlist URL."
                    } else {
                        "Loaded ${channels.size} entries. Tap a channel to play."
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text =
                        "Could not load playlist: ${e.localizedMessage ?: "network error"}"
                }
            }
        }
    }

    private fun parseM3u(text: String): MutableList<Channel> {
        val result = mutableListOf<Channel>()
        var name = "Channel"
        var group = "Live TV"

        text.lineSequence().forEach { raw ->
            val line = raw.trim()

            if (line.startsWith("#EXTINF", true)) {
                name = line.substringAfterLast(',')
                    .trim()
                    .ifBlank { "Channel ${result.size + 1}" }

                group = Regex(
                    "group-title=\"([^\"]*)\"",
                    RegexOption.IGNORE_CASE
                ).find(line)?.groupValues?.getOrNull(1) ?: "Live TV"
            } else if (
                line.startsWith("http://") ||
                line.startsWith("https://")
            ) {
                result.add(Channel(name, line, group))
                name = "Channel ${result.size + 1}"
                group = "Live TV"
            }
        }
        return result
    }

    private fun saveChannels() {
        val arr = JSONArray()

        channels.forEach { c ->
            arr.put(
                org.json.JSONObject()
                    .put("name", c.name)
                    .put("url", c.url)
                    .put("group", c.group)
            )
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString("channels_json", arr.toString())
            .apply()
    }

    private fun renderChannels() {
        list.removeAllViews()

        channels.forEach { channel ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14))
                setBackgroundColor(PANEL)
            }

            row.addView(TextView(this).apply {
                text = channel.name
                textSize = 16f
                setTextColor(Color.WHITE)
                setTypeface(null, 1)
            })

            row.addView(TextView(this).apply {
                text = channel.group
                textSize = 12f
                setTextColor(CYAN)
                setPadding(0, dp(4), 0, 0)
            })

            row.setOnClickListener {
                startActivity(
                    Intent(this, PlayerActivity::class.java)
                        .putExtra("name", channel.name)
                        .putExtra("url", channel.url)
                )
            }

            list.addView(
                row,
                LinearLayout.LayoutParams(
                    -1,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
            )
        }
    }

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
