package com.tayebi7.fennecplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tayebi7.fennecplayer.data.Playlist
import com.tayebi7.fennecplayer.data.PlaylistStore
import com.tayebi7.fennecplayer.data.PlaylistType
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.data.UrlUtils
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** إضافة قائمة تشغيل: رابط M3U أو Xtream Codes أو ملف M3U محلي. */
class ActivationActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvFileName: TextView
    private lateinit var etPlaylistName: EditText
    private lateinit var etServerUrl: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etM3uUrl: EditText
    private lateinit var btnLoginConnect: Button
    private lateinit var tabM3u: Button
    private lateinit var tabXtream: Button
    private lateinit var tabFile: Button
    private lateinit var colServer: View
    private lateinit var layoutCreds: View
    private lateinit var layoutM3u: View
    private lateinit var layoutFile: View
    private lateinit var tvM3uHint: View

    private var mode = TAB_XTREAM
    private var pickedUri: Uri? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedUri = uri
            tvFileName.text = displayName(uri)
            if (etPlaylistName.text.isNullOrBlank()) etPlaylistName.setText(displayName(uri).substringBeforeLast('.'))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activation)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()

        bindViews()
        etPlaylistName.setText("Fennec IPTV")
        setMode(intent.getIntExtra(EXTRA_TAB, TAB_XTREAM))

        tabM3u.setOnClickListener { setMode(TAB_M3U) }
        tabXtream.setOnClickListener { setMode(TAB_XTREAM) }
        tabFile.setOnClickListener { setMode(TAB_FILE) }
        findViewById<View>(R.id.btnChooseFile).setOnClickListener {
            pickFile.launch(arrayOf("audio/x-mpegurl", "application/x-mpegurl", "application/vnd.apple.mpegurl", "text/*", "application/octet-stream", "*/*"))
        }
        findViewById<View>(R.id.btnBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnLoginConnect.setOnClickListener { connect() }

        // بدون قائمة محفوظة: زر الرجوع يغلق التطبيق بدل العودة لحلقة الإضافة
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (PlaylistStore.load(this@ActivationActivity) == null) finishAffinity() else finish()
            }
        })
    }

    private fun bindViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvFileName = findViewById(R.id.tvFileName)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        etServerUrl = findViewById(R.id.etServerUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnLoginConnect = findViewById(R.id.btnLoginConnect)
        tabM3u = findViewById(R.id.btnTabM3u)
        tabXtream = findViewById(R.id.btnTabXtream)
        tabFile = findViewById(R.id.btnTabFile)
        colServer = findViewById(R.id.colServer)
        layoutCreds = findViewById(R.id.layoutCreds)
        layoutM3u = findViewById(R.id.layoutM3u)
        layoutFile = findViewById(R.id.layoutFile)
        tvM3uHint = findViewById(R.id.tvM3uHint)
    }

    private fun setMode(tab: Int) {
        mode = tab
        colServer.visibility = if (tab == TAB_XTREAM) View.VISIBLE else View.GONE
        layoutCreds.visibility = if (tab == TAB_XTREAM) View.VISIBLE else View.GONE
        layoutM3u.visibility = if (tab == TAB_M3U) View.VISIBLE else View.GONE
        tvM3uHint.visibility = if (tab == TAB_M3U) View.VISIBLE else View.GONE
        layoutFile.visibility = if (tab == TAB_FILE) View.VISIBLE else View.GONE
        btnLoginConnect.setText(if (tab == TAB_XTREAM) R.string.btn_login else R.string.btn_add)
        val on = 0xFF22C7FF.toInt()
        val off = 0xFFB7D4EE.toInt()
        listOf(tabM3u to TAB_M3U, tabXtream to TAB_XTREAM, tabFile to TAB_FILE).forEach { (b, t) ->
            b.isActivated = t == tab
            b.setTextColor(if (t == tab) on else off)
        }
        tvStatus.visibility = View.GONE
    }

    private fun showError(msg: String) {
        tvStatus.text = msg
        tvStatus.visibility = View.VISIBLE
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: (uri.lastPathSegment ?: "playlist.m3u")

    private fun connect() {
        val name = etPlaylistName.text.toString().trim().ifEmpty { "Fennec TV" }

        // نبني القائمة (للملف المحلي ننسخه أولاً داخل مجلد التطبيق)
        when (mode) {
            TAB_XTREAM -> {
                val server = UrlUtils.normalizeServer(etServerUrl.text.toString())
                val user = etUsername.text.toString().trim()
                val pass = etPassword.text.toString().trim()
                if (server.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                    showError(getString(R.string.err_xtream_fields)); return
                }
                verifyAndSave(Playlist(name = name, type = PlaylistType.XTREAM, server = server, username = user, password = pass))
            }
            TAB_M3U -> {
                val url = UrlUtils.normalizeUrl(etM3uUrl.text.toString())
                if (url.isEmpty()) { showError(getString(R.string.err_m3u_empty)); return }
                val x = UrlUtils.xtreamFromM3u(url)
                val playlist = if (x != null) Playlist(name = name, type = PlaylistType.XTREAM, server = x.first, username = x.second, password = x.third)
                else Playlist(name = name, type = PlaylistType.M3U, m3uUrl = url)
                verifyAndSave(playlist)
            }
            else -> {
                val uri = pickedUri
                if (uri == null) { showError(getString(R.string.err_file_none)); return }
                setBusy(true)
                lifecycleScope.launch {
                    val local = withContext(Dispatchers.IO) { copyToApp(uri) }
                    if (local == null) {
                        setBusy(false)
                        showError(getString(R.string.err_file_read))
                    } else {
                        verifyAndSave(Playlist(name = name, type = PlaylistType.LOCAL, m3uUrl = local.absolutePath), cleanupOnFail = local)
                    }
                }
            }
        }
    }

    private fun copyToApp(uri: Uri): File? = runCatching {
        val dest = File(PlaylistStore.localDir(this), "${System.currentTimeMillis()}.m3u")
        contentResolver.openInputStream(uri)!!.use { input -> dest.outputStream().use { input.copyTo(it) } }
        dest
    }.getOrNull()

    private fun setBusy(busy: Boolean) {
        btnLoginConnect.isEnabled = !busy
        if (busy) btnLoginConnect.setText(R.string.verifying)
        else btnLoginConnect.setText(if (mode == TAB_XTREAM) R.string.btn_login else R.string.btn_add)
        if (busy) tvStatus.visibility = View.GONE
    }

    private fun verifyAndSave(playlist: Playlist, cleanupOnFail: File? = null) {
        setBusy(true)
        lifecycleScope.launch {
            val error = Repo.verify(this@ActivationActivity, playlist)
            setBusy(false)
            if (error != null) {
                cleanupOnFail?.delete()
                showError(error)
                return@launch
            }
            PlaylistStore.save(this@ActivationActivity, playlist)
            Toast.makeText(this@ActivationActivity, R.string.saved, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@ActivationActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
        }
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_M3U = 0
        const val TAB_XTREAM = 1
        const val TAB_FILE = 2
    }
}
