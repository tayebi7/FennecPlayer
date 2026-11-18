package com.tayebi7.fennecplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tayebi7.fennecplayer.data.Playlist
import com.tayebi7.fennecplayer.data.PlaylistStore
import com.tayebi7.fennecplayer.data.PlaylistType
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.data.UrlUtils
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding
import kotlinx.coroutines.launch

/** شاشة إضافة قائمة تشغيل (Xtream أو رابط M3U). */
class ActivationActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etPlaylistName: EditText
    private lateinit var etServerUrl: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etM3uUrl: EditText
    private lateinit var btnLoginConnect: Button
    private lateinit var btnTabXtream: Button
    private lateinit var btnTabM3u: Button
    private lateinit var colServer: View
    private lateinit var layoutCreds: View
    private lateinit var layoutM3u: View
    private lateinit var tvM3uHint: View

    private var mode = PlaylistType.XTREAM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activation)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()

        initViews()
        loadSavedCredentials()
        setupListeners()

        // بدون قائمة محفوظة: زر الرجوع يغلق التطبيق بدل العودة لحلقة التفعيل
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (PlaylistStore.load(this@ActivationActivity) == null) finishAffinity() else finish()
            }
        })
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        etServerUrl = findViewById(R.id.etServerUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnLoginConnect = findViewById(R.id.btnLoginConnect)
        btnTabXtream = findViewById(R.id.btnTabXtream)
        btnTabM3u = findViewById(R.id.btnTabM3u)
        colServer = findViewById(R.id.colServer)
        layoutCreds = findViewById(R.id.layoutCreds)
        tvM3uHint = findViewById(R.id.tvM3uHint)
        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        layoutM3u = findViewById(R.id.layoutM3u)
    }

    private fun loadSavedCredentials() {
        val saved = PlaylistStore.load(this)
        etPlaylistName.setText(saved?.name ?: "Fennec IPTV")
        if (saved != null) {
            etServerUrl.setText(saved.server)
            etUsername.setText(saved.username)
            etPassword.setText(saved.password)
            etM3uUrl.setText(saved.m3uUrl)
            setMode(saved.type)
        } else {
            setMode(PlaylistType.XTREAM)
        }
    }

    private fun setMode(type: PlaylistType) {
        mode = type
        val x = if (type == PlaylistType.XTREAM) View.VISIBLE else View.GONE
        colServer.visibility = x
        layoutCreds.visibility = x
        tvM3uHint.visibility = if (type == PlaylistType.M3U) View.VISIBLE else View.GONE
        layoutM3u.visibility = if (type == PlaylistType.M3U) View.VISIBLE else View.GONE
        btnTabXtream.isActivated = type == PlaylistType.XTREAM
        btnTabM3u.isActivated = type == PlaylistType.M3U
        val on = 0xFF1EB6FF.toInt()
        val off = 0xFFB7D4EE.toInt()
        btnTabXtream.setTextColor(if (btnTabXtream.isActivated) on else off)
        btnTabM3u.setTextColor(if (btnTabM3u.isActivated) on else off)
        tvStatus.visibility = View.GONE
    }

    private fun setupListeners() {
        btnTabXtream.setOnClickListener { setMode(PlaylistType.XTREAM) }
        btnTabM3u.setOnClickListener { setMode(PlaylistType.M3U) }
        btnLoginConnect.setOnClickListener { connect() }
    }

    private fun showError(msg: String) {
        tvStatus.text = msg
        tvStatus.visibility = View.VISIBLE
    }

    private fun connect() {
        val name = etPlaylistName.text.toString().trim().ifEmpty { "Fennec TV" }

        val playlist: Playlist = if (mode == PlaylistType.XTREAM) {
            val server = UrlUtils.normalizeServer(etServerUrl.text.toString())
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            if (server.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                showError("يرجى إدخال رابط السيرفر واسم المستخدم وكلمة المرور")
                return
            }
            Playlist(name, PlaylistType.XTREAM, server, user, pass)
        } else {
            val url = UrlUtils.normalizeUrl(etM3uUrl.text.toString())
            if (url.isEmpty()) {
                showError("يرجى إدخال رابط ملف M3U")
                return
            }
            val x = UrlUtils.xtreamFromM3u(url)
            if (x != null) Playlist(name, PlaylistType.XTREAM, x.first, x.second, x.third)
            else Playlist(name, PlaylistType.M3U, m3uUrl = url)
        }

        tvStatus.visibility = View.GONE
        btnLoginConnect.isEnabled = false
        btnLoginConnect.text = "جاري التحقق..."

        lifecycleScope.launch {
            val error = Repo.verify(playlist)
            btnLoginConnect.isEnabled = true
            btnLoginConnect.text = "تسجيل الدخول"
            if (error != null) {
                showError(error)
                return@launch
            }

            PlaylistStore.save(this@ActivationActivity, playlist)
            Toast.makeText(this@ActivationActivity, "تم حفظ القائمة بنجاح", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@ActivationActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
        }
    }
}
