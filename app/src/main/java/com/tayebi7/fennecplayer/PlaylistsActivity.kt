package com.tayebi7.fennecplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tayebi7.fennecplayer.data.Playlist
import com.tayebi7.fennecplayer.data.PlaylistStore
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.ui.PlaylistAdapter
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding

/** إدارة قوائم التشغيل: تبديل القائمة المفعّلة، إضافة، وحذف (ضغطة مطولة). */
class PlaylistsActivity : AppCompatActivity() {

    private val adapter = PlaylistAdapter(
        onClick = { p -> if (p == null) openAdd() else activate(p) },
        onLongClick = { confirmDelete(it) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        val rv = findViewById<RecyclerView>(R.id.rvPlaylists)
        val dm = resources.displayMetrics
        val span = (dm.widthPixels / dm.density / 200f).toInt().coerceIn(2, 5)
        rv.layoutManager = GridLayoutManager(this, span)
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(PlaylistStore.all(this), PlaylistStore.load(this)?.id)
    }

    private fun openAdd() = startActivity(Intent(this, ActivationActivity::class.java))

    private fun activate(p: Playlist) {
        PlaylistStore.setActive(this, p.id)
        Repo.invalidate()
        finish()
    }

    private fun confirmDelete(p: Playlist) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.playlist_delete, p.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                PlaylistStore.remove(this, p.id)
                Repo.invalidate()
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
