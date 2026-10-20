package com.tayebi7.fennecplayer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.ui.PlayerView
import com.tayebi7.fennecplayer.network.CategoryModel
import com.tayebi7.fennecplayer.network.LiveStreamModel
import com.tayebi7.fennecplayer.network.XtreamApiService
import com.tayebi7.fennecplayer.player.FennecPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveTvActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var playerView: PlayerView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvCurrentChannelName: TextView

    private var serverUrl: String = ""
    private var username: String = ""
    private var password: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tv)

        initViews()
        loadCredentials()
        fetchCategories()
    }

    private fun initViews() {
        rvCategories = findViewById(R.id.rvCategories)
        rvChannels = findViewById(R.id.rvChannels)
        playerView = findViewById(R.id.playerView)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)
        tvCurrentChannelName = findViewById(R.id.tvCurrentChannelName)

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvChannels.layoutManager = LinearLayoutManager(this)
    }

    private fun loadCredentials() {
        val prefs = getSharedPreferences("fennec_prefs", Context.MODE_PRIVATE)
        serverUrl = prefs.getString("server_url", "") ?: ""
        username = prefs.getString("username", "") ?: ""
        password = prefs.getString("password", "") ?: ""
    }

    private fun fetchCategories() {
        if (serverUrl.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = XtreamApiService.create(serverUrl)
                val response = api.getLiveCategories(username, password)
                if (response.isSuccessful && response.body() != null) {
                    val categories = response.body()!!
                    withContext(Dispatchers.Main) {
                        rvCategories.adapter = SimpleCategoryAdapter(categories) { category ->
                            fetchChannels(category.categoryId, category.categoryName)
                        }
                        if (categories.isNotEmpty()) {
                            fetchChannels(categories[0].categoryId, categories[0].categoryName)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LiveTvActivity, "فشل تحميل الأقسام", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchChannels(categoryId: String, categoryName: String) {
        tvCategoryTitle.text = "القسم: $categoryName"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = XtreamApiService.create(serverUrl)
                val response = api.getLiveStreams(username, password, categoryId = categoryId)
                if (response.isSuccessful && response.body() != null) {
                    val channels = response.body()!!
                    withContext(Dispatchers.Main) {
                        rvChannels.adapter = SimpleChannelAdapter(channels) { channel ->
                            playChannel(channel)
                        }
                        if (channels.isNotEmpty()) {
                            playChannel(channels[0])
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LiveTvActivity, "فشل تحميل القنوات", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playChannel(channel: LiveStreamModel) {
        tvCurrentChannelName.text = channel.name
        val streamUrl = XtreamApiService.buildLiveStreamUrl(serverUrl, username, password, channel.streamId)
        
        FennecPlayerManager.initializePlayer(this, playerView, streamUrl) { errorMsg ->
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FennecPlayerManager.releasePlayer()
    }

    // محولات بسيطة لعرض العناصر داخل القوائم
    private class SimpleCategoryAdapter(
        private val list: List<CategoryModel>,
        private val onClick: (CategoryModel) -> Unit
    ) : RecyclerView.Adapter<SimpleCategoryAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tv: TextView = view.findViewById(android.R.id.text1)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            view.setBackgroundColor(0xFF131527.toInt())
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tv.text = item.categoryName
            holder.tv.setTextColor(0xFFFFFFFF.toInt())
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }

    private class SimpleChannelAdapter(
        private val list: List<LiveStreamModel>,
        private val onClick: (LiveStreamModel) -> Unit
    ) : RecyclerView.Adapter<SimpleChannelAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tv: TextView = view.findViewById(android.R.id.text1)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            view.setBackgroundColor(0xFF1A1C33.toInt())
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tv.text = item.name
            holder.tv.setTextColor(0xFFFFA726.toInt())
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }
}
