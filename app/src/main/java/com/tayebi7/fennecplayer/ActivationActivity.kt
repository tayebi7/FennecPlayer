package com.tayebi7.fennecplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import com.tayebi7.fennecplayer.utils.IboDeviceUtils
import kotlinx.coroutines.launch

class ActivationActivity : AppCompatActivity() {

    private lateinit var tvMacAddress: TextView
    private lateinit var tvDeviceKey: TextView
    private lateinit var tvStatus: TextView
    private lateinit var ivQrCode: ImageView
    private lateinit var etPlaylistName: EditText
    private lateinit var etServerUrl: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etM3uUrl: EditText
    private lateinit var etCustomMac: EditText
    private lateinit var btnLoginConnect: Button
    private lateinit var btnReloadMac: Button
    private lateinit var btnTabXtream: Button
    private lateinit var btnTabM3u: Button
    private lateinit var layoutXtream: View
    private lateinit var layoutM3u: View

    private var mode = PlaylistType.XTREAM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activation)

        initViews()
        loadDeviceIdentity()
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
        tvMacAddress = findViewById(R.id.tvMacAddress)
        tvDeviceKey = findViewById(R.id.tvDeviceKey)
        tvStatus = findViewById(R.id.tvStatus)
        ivQrCode = findViewById(R.id.ivQrCode)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        etServerUrl = findViewById(R.id.etServerUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etM3uUrl = findViewById(R.id.etM3uUrl)
        etCustomMac = findViewById(R.id.etCustomMac)
        btnLoginConnect = findViewById(R.id.btnLoginConnect)
        btnReloadMac = findViewById(R.id.btnReloadMac)
        btnTabXtream = findViewById(R.id.btnTabXtream)
        btnTabM3u = findViewById(R.id.btnTabM3u)
        layoutXtream = findViewById(R.id.layoutXtream)
        layoutM3u = findViewById(R.id.layoutM3u)
    }

    private fun loadDeviceIdentity() {
        val mac = IboDeviceUtils.getMacAddress(this)
        val key = IboDeviceUtils.getDeviceKey(this)
        tvMacAddress.text = mac
        tvDeviceKey.text = key
        etCustomMac.setText(mac)

        IboDeviceUtils.generateQrCodeBitmap("Fennec Player\nMAC: $mac\nKey: $key", 400)
            ?.let { ivQrCode.setImageBitmap(it) }
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
        layoutXtream.visibility = if (type == PlaylistType.XTREAM) View.VISIBLE else View.GONE
        layoutM3u.visibility = if (type == PlaylistType.M3U) View.VISIBLE else View.GONE
        btnTabXtream.isActivated = type == PlaylistType.XTREAM
        btnTabM3u.isActivated = type == PlaylistType.M3U
        btnTabXtream.setTextColor(if (btnTabXtream.isActivated) 0xFF101827.toInt() else 0xFFFFFFFF.toInt())
        btnTabM3u.setTextColor(if (btnTabM3u.isActivated) 0xFF101827.toInt() else 0xFFFFFFFF.toInt())
        tvStatus.visibility = View.GONE
    }

    private fun setupListeners() {
        btnTabXtream.setOnClickListener { setMode(PlaylistType.XTREAM) }
        btnTabM3u.setOnClickListener { setMode(PlaylistType.M3U) }

        btnReloadMac.setOnClickListener {
            loadDeviceIdentity()
            Toast.makeText(this, "تم تحديث بيانات الجهاز", Toast.LENGTH_SHORT).show()
        }

        btnLoginConnect.setOnClickListener { connect() }
    }

    private fun showError(msg: String) {
        tvStatus.text = msg
        tvStatus.visibility = View.VISIBLE
    }

    private fun connect() {
        val name = etPlaylistName.text.toString().trim().ifEmpty { "Fennec TV" }
        val customMac = etCustomMac.text.toString().trim()

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
            if (customMac.isNotEmpty()) {
                getSharedPreferences(PlaylistStore.PREFS, Context.MODE_PRIVATE)
                    .edit().putString("device_mac", customMac).apply()
            }
            Toast.makeText(this@ActivationActivity, "تم حفظ القائمة بنجاح", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@ActivationActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
        }
    }
}
