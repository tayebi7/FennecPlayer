package com.tayebi7.fennecplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tayebi7.fennecplayer.utils.IboDeviceUtils

class ActivationActivity : AppCompatActivity() {

    private lateinit var tvMacAddress: TextView
    private lateinit var tvDeviceKey: TextView
    private lateinit var ivQrCode: ImageView
    private lateinit var etPlaylistName: EditText
    private lateinit var etServerUrl: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etCustomMac: EditText
    private lateinit var btnLoginConnect: Button
    private lateinit var btnReloadMac: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activation)

        initViews()
        loadDeviceIdentity()
        loadSavedCredentials()
        setupListeners()
    }

    private fun initViews() {
        tvMacAddress = findViewById(R.id.tvMacAddress)
        tvDeviceKey = findViewById(R.id.tvDeviceKey)
        ivQrCode = findViewById(R.id.ivQrCode)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        etServerUrl = findViewById(R.id.etServerUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etCustomMac = findViewById(R.id.etCustomMac)
        btnLoginConnect = findViewById(R.id.btnLoginConnect)
        btnReloadMac = findViewById(R.id.btnReloadMac)
    }

    private fun loadDeviceIdentity() {
        val mac = IboDeviceUtils.getMacAddress(this)
        val key = IboDeviceUtils.getDeviceKey(this)

        tvMacAddress.text = mac
        tvDeviceKey.text = key
        etCustomMac.setText(mac)

        val qrPayload = "https://fennecplayer.com/activate?mac=$mac&key=$key"
        val qrBitmap = IboDeviceUtils.generateQrCodeBitmap(qrPayload, 400)
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap)
        }
    }

    private fun loadSavedCredentials() {
        val prefs = getSharedPreferences("fennec_prefs", Context.MODE_PRIVATE)
        etPlaylistName.setText(prefs.getString("playlist_name", "Fennec IPTV"))
        etServerUrl.setText(prefs.getString("server_url", ""))
        etUsername.setText(prefs.getString("username", ""))
        etPassword.setText(prefs.getString("password", ""))
    }

    private fun setupListeners() {
        btnReloadMac.setOnClickListener {
            loadDeviceIdentity()
            Toast.makeText(this, "تم تحديث بيانات الجهاز", Toast.LENGTH_SHORT).show()
        }

        btnLoginConnect.setOnClickListener {
            val playlistName = etPlaylistName.text.toString().trim()
            val serverUrl = etServerUrl.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val customMac = etCustomMac.text.toString().trim()

            if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال رابط السيرفر واسم المستخدم وكلمة المرور", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("fennec_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("playlist_name", playlistName.ifEmpty { "Fennec TV" })
                putString("server_url", serverUrl)
                putString("username", username)
                putString("password", password)
                if (customMac.isNotEmpty()) {
                    putString("device_mac", customMac)
                }
                apply()
            }

            loadDeviceIdentity()

            Toast.makeText(this, "تم حفظ البيانات بنجاح! جاري الانتقال...", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
