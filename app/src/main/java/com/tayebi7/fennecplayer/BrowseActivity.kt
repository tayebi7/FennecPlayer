package com.tayebi7.fennecplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tayebi7.fennecplayer.data.Catalog
import com.tayebi7.fennecplayer.data.Category
import com.tayebi7.fennecplayer.data.ContentType
import com.tayebi7.fennecplayer.data.Entry
import com.tayebi7.fennecplayer.data.Episode
import com.tayebi7.fennecplayer.data.Favorites
import com.tayebi7.fennecplayer.data.PlaylistStore
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.ui.CategoryAdapter
import com.tayebi7.fennecplayer.ui.PosterAdapter
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding
import kotlinx.coroutines.launch

/** شاشة الأفلام والمسلسلات: أقسام على اليسار وشبكة بوسترات، مع المفضلة (ضغطة مطولة). */
class BrowseActivity : AppCompatActivity() {

    private lateinit var type: ContentType
    private lateinit var rvCategories: RecyclerView
    private lateinit var rvItems: RecyclerView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var progress: ProgressBar

    private val categoryAdapter = CategoryAdapter { selectCategory(it) }
    private val posterAdapter = PosterAdapter({ open(it) }, { toggleFavorite(it) })

    private var catalog: Catalog = Catalog.EMPTY
    private var currentCategory: Category? = null
    private var query = ""
    private var playlistId = ""
    private var favIds: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()

        type = runCatching { ContentType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: "") }
            .getOrDefault(ContentType.MOVIE)
        playlistId = PlaylistStore.load(this)?.id.orEmpty()

        rvCategories = findViewById(R.id.rvCategories)
        rvItems = findViewById(R.id.rvItems)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)
        progress = findViewById(R.id.progress)

        findViewById<TextView>(R.id.tvTitle).setText(
            if (type == ContentType.SERIES) R.string.home_series else R.string.home_movies
        )
        findViewById<ImageView>(R.id.ivTitleIcon)
            .setImageResource(if (type == ContentType.SERIES) R.drawable.ic_series else R.drawable.ic_movie)

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = categoryAdapter
        rvItems.layoutManager = GridLayoutManager(this, spanCount())
        rvItems.adapter = posterAdapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim().orEmpty()
                applyFilter()
            }
        })

        loadData()
    }

    private fun spanCount(): Int {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        return (widthDp * 0.7f / 130f).toInt().coerceIn(3, 8)
    }

    private fun loadData() {
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            try {
                catalog = Repo.catalog(this@BrowseActivity, type)
            } catch (e: Exception) {
                Toast.makeText(this@BrowseActivity, getString(R.string.load_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
            progress.visibility = View.GONE
            favIds = Favorites.ids(this@BrowseActivity, playlistId, type)
            posterAdapter.favIds = favIds

            val all = Category("*", getString(R.string.all), catalog.entries.size)
            val fav = Category(FAV_ID, getString(R.string.fav_category), favIds.size)
            categoryAdapter.submit(listOf(all, fav) + catalog.categories.filter { it.count > 0 })
            if (intent.getBooleanExtra(EXTRA_FAV, false)) {
                categoryAdapter.select(1)
                selectCategory(fav)
            } else selectCategory(all)
        }
    }

    private fun selectCategory(category: Category) {
        currentCategory = category
        tvCategoryTitle.text = category.name
        applyFilter()
    }

    private fun toggleFavorite(entry: Entry) {
        val now = Favorites.toggle(this, playlistId, type, entry.id)
        favIds = Favorites.ids(this, playlistId, type)
        posterAdapter.favIds = favIds
        Toast.makeText(this, if (now) R.string.fav_added else R.string.fav_removed, Toast.LENGTH_SHORT).show()
        if (currentCategory?.id == FAV_ID) applyFilter()
    }

    private fun applyFilter() {
        val cat = currentCategory ?: return
        val q = query
        val list = catalog.entries.filter {
            (cat.id == "*" || (cat.id == FAV_ID && it.id in favIds) || it.categoryId == cat.id) &&
                (q.isEmpty() || it.name.contains(q, ignoreCase = true))
        }
        posterAdapter.submit(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = getString(
            when {
                catalog.entries.isEmpty() -> R.string.no_content
                cat.id == FAV_ID && favIds.isEmpty() -> R.string.no_favorites
                else -> R.string.no_results
            }
        )
    }

    private fun open(entry: Entry) {
        val url = entry.url
        if (url != null) {
            PlayerActivity.start(this, arrayListOf(entry.name), arrayListOf(url), 0)
            return
        }
        // مسلسل Xtream: نجلب الحلقات ثم نعرضها
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            val episodes = try {
                Repo.episodes(this@BrowseActivity, entry.id)
            } catch (e: Exception) {
                Toast.makeText(this@BrowseActivity, getString(R.string.episodes_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                emptyList()
            }
            progress.visibility = View.GONE
            if (episodes.isEmpty()) {
                Toast.makeText(this@BrowseActivity, R.string.no_episodes, Toast.LENGTH_SHORT).show()
            } else {
                showEpisodes(entry, episodes)
            }
        }
    }

    private fun showEpisodes(series: Entry, episodes: List<Episode>) {
        val labels = episodes.map { "S%02d  E%02d  •  %s".format(it.season, it.number, it.title) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(series.name)
            .setItems(labels) { _, which ->
                // نمرر كل الحلقات ليتم الانتقال التلقائي للحلقة التالية
                PlayerActivity.start(
                    this,
                    ArrayList(episodes.map { "${series.name} - ${it.title}" }),
                    ArrayList(episodes.map { it.url }),
                    which
                )
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    companion object {
        private const val EXTRA_TYPE = "type"
        const val EXTRA_FAV = "fav"
        private const val FAV_ID = "__fav__"

        fun start(context: Context, type: ContentType, fav: Boolean = false) {
            context.startActivity(
                Intent(context, BrowseActivity::class.java)
                    .putExtra(EXTRA_TYPE, type.name)
                    .putExtra(EXTRA_FAV, fav)
            )
        }
    }
}
