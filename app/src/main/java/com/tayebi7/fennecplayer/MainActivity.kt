package com.tayebi7.fennecplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tayebi7.fennecplayer.network.XtreamApiService
import com.tayebi7.fennecplayer.utils.IboDeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvActivePlaylist: TextView
    private lateinit var tvExpiration: TextView
    private lateinit var tvFooterMac: TextView
    private lateinit var tvLiveCount: TextView

    private lateinit var cardLiveTv: LinearLayout
    private lateinit var cardMovies: LinearLayout
    private lateinit var cardSeries: LinearLayout

    private lateinit var btnChangePlaylist: Button
    private lateinit var btnReloadData: Button
    private lateinit var btnExitApp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        cardLiveTv.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        checkAndLoadPlaylist()
    }

    private fun initViews() {
        tvActivePlaylist = findViewById(R.id.tvActivePlaylist)
        tvExpiration = findViewById(R.id.tvExpiration)
        tvFooterMac = findViewById(R.id.tvFooterMac)
        tvLiveCount = findViewById(R.id.tvLiveCount)

        cardLiveTv = findViewById(R.id.cardLiveTv)
        cardMovies = findViewById(R.id.cardMovies)
        cardSeries = findViewById(R.id.cardSeries)

        btnChangePlaylist = findViewById(R.id.btnChangePlaylist)
        btnReloadData = findViewById(R.id.btnReloadData)
        btnExitApp = findViewById(R.id.btnExitApp)
    }

    private fun checkAndLoadPlaylist() {
        val prefs = getSharedPreferences("fennec_prefs", Context.MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", "") ?: ""
        val username = prefs.getString("username", "") ?: ""
        val password = prefs.getString("password", "") ?: ""
        val playlistName = prefs.getString("playlist_name", "Fennec TV") ?: "Fennec TV"

        val mac = IboDeviceUtils.getMacAddress(this)
        tvFooterMac.text = "MAC: $mac"
        tvActivePlaylist.text = "Playlist: $playlistName"

        if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
            startActivity(Intent(this, ActivationActivity::class.java))
            return
        }

        syncWithXtreamServer(serverUrl, username, password)
    }

    private fun syncWithXtreamServer(serverUrl: String, username: String, password: String) {
        tvExpiration.text = "جاري الاتصال بالسيرفر..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = XtreamApiService.create(serverUrl)
                val loginResponse = api.login(username, password)

                if (loginResponse.isSuccessful && loginResponse.body()?.userInfo != null) {
                    val userInfo = loginResponse.body()!!.userInfo!!
                    val expFormatted = formatExpiryDate(userInfo.expDate)

                    val streamsResponse = api.getLiveStreams(username, password)
                    val liveCount = streamsResponse.body()?.size ?: 0

                    withContext(Dispatchers.Main) {
                        tvExpiration.text = "الصلاحية: $expFormatted (${userInfo.status})"
                        tvLiveCount.text = "$liveCount قناة مباشرة"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        tvExpiration.text = "بيانات الاشتراك غير صحيحة"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvExpiration.text = "تعذر الاتصال بالسيرفر"
                }
            }
        }
    }

    private fun formatExpiryDate(timestampStr: String?): String {
        if (timestampStr.isNullOrEmpty()) return "غير محدود"
        return try {
            val seconds = timestampStr.toLong()
            val date = Date(seconds * 1000L)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        } catch (e: Exception) {
            "غير محدود"
        }
    }

    private fun setupListeners() {
        cardLiveTv.setOnClickListener {
            // الانتقال إلى شاشة البث المباشر
            startActivity(Intent(this, LiveTvActivity::class.java))
        }

        cardMovies.setOnClickListener {
            Toast.makeText(this, "مكتبة الأفلام قيد التفعيل...", Toast.LENGTH_SHORT).show()
        }

        cardSeries.setOnClickListener {
            Toast.makeText(this, "مكتبة المسلسلات قيد التفعيل...", Toast.LENGTH_SHORT).show()
        }

        btnChangePlaylist.setOnClickListener {
            startActivity(Intent(this, ActivationActivity::class.java))
        }

        btnReloadData.setOnClickListener {
            checkAndLoadPlaylist()
            Toast.makeText(this, "جاري تحديث البيانات...", Toast.LENGTH_SHORT).show()
        }

        btnExitApp.setOnClickListener {
            finishAffinity()
        }
    }
}
