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
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.ui.CategoryAdapter
import com.tayebi7.fennecplayer.ui.PosterAdapter
import kotlinx.coroutines.launch

/** شاشة الأفلام والمسلسلات: أقسام على اليسار وشبكة بوسترات على اليمين. */
class BrowseActivity : AppCompatActivity() {

    private lateinit var type: ContentType
    private lateinit var rvCategories: RecyclerView
    private lateinit var rvItems: RecyclerView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var progress: ProgressBar

    private val categoryAdapter = CategoryAdapter { selectCategory(it) }
    private val posterAdapter = PosterAdapter { open(it) }

    private var catalog: Catalog = Catalog.EMPTY
    private var currentCategory: Category? = null
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        type = runCatching { ContentType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: "") }
            .getOrDefault(ContentType.MOVIE)

        rvCategories = findViewById(R.id.rvCategories)
        rvItems = findViewById(R.id.rvItems)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)
        progress = findViewById(R.id.progress)

        findViewById<TextView>(R.id.tvTitle).text = if (type == ContentType.SERIES) "المسلسلات" else "الأفلام"
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
                Toast.makeText(this@BrowseActivity, "فشل التحميل: ${e.message}", Toast.LENGTH_LONG).show()
            }
            progress.visibility = View.GONE

            val all = Category("*", "الكل", catalog.entries.size)
            categoryAdapter.submit(listOf(all) + catalog.categories.filter { it.count > 0 })
            selectCategory(all)
        }
    }

    private fun selectCategory(category: Category) {
        currentCategory = category
        tvCategoryTitle.text = category.name
        applyFilter()
    }

    private fun applyFilter() {
        val cat = currentCategory ?: return
        val q = query
        val list = catalog.entries.filter {
            (cat.id == "*" || it.categoryId == cat.id) &&
                (q.isEmpty() || it.name.contains(q, ignoreCase = true))
        }
        posterAdapter.submit(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = if (catalog.entries.isEmpty()) "لا يوجد محتوى" else "لا توجد نتائج"
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
                Toast.makeText(this@BrowseActivity, "فشل تحميل الحلقات: ${e.message}", Toast.LENGTH_LONG).show()
                emptyList()
            }
            progress.visibility = View.GONE
            if (episodes.isEmpty()) {
                Toast.makeText(this@BrowseActivity, "لا توجد حلقات متاحة", Toast.LENGTH_SHORT).show()
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
            .setNegativeButton("إغلاق", null)
            .show()
    }

    companion object {
        private const val EXTRA_TYPE = "type"

        fun start(context: Context, type: ContentType) {
            context.startActivity(Intent(context, BrowseActivity::class.java).putExtra(EXTRA_TYPE, type.name))
        }
    }
}
