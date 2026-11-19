package com.tayebi7.fennecplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tayebi7.fennecplayer.R
import com.tayebi7.fennecplayer.data.Category
import com.tayebi7.fennecplayer.data.Entry
import com.tayebi7.fennecplayer.data.Playlist
import com.tayebi7.fennecplayer.data.PlaylistType

private fun ImageView.loadImage(url: String?) {
    if (url.isNullOrBlank()) {
        Glide.with(this).clear(this)
        setImageResource(R.drawable.ic_placeholder)
    } else {
        Glide.with(this).load(url)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(this)
    }
}

class CategoryAdapter(
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var items: List<Category> = emptyList()
    private var selected = 0

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
    }

    fun submit(list: List<Category>) {
        items = list
        selected = 0
        notifyDataSetChanged()
    }

    fun select(index: Int) {
        if (index !in items.indices) return
        val old = selected
        selected = index
        notifyItemChanged(old)
        notifyItemChanged(index)
    }

    fun categoryAt(index: Int): Category? = items.getOrNull(index)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.name.text = if (c.count > 0) "${c.name}  (${c.count})" else c.name
        holder.itemView.isActivated = position == selected
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val old = selected
            selected = pos
            notifyItemChanged(old)
            notifyItemChanged(pos)
            onClick(items[pos])
        }
    }

    override fun getItemCount() = items.size
}

class ChannelAdapter(
    private val onClick: (Entry) -> Unit,
    private val onLongClick: (Entry) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private var items: List<Entry> = emptyList()
    var playingId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var favIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.ivLogo)
        val name: TextView = view.findViewById(R.id.tvName)
        val number: TextView = view.findViewById(R.id.tvNumber)
        val fav: ImageView = view.findViewById(R.id.ivFav)
    }

    fun submit(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.name.text = e.name
        holder.logo.loadImage(e.logo)
        holder.number.text = (position + 1).toString()
        holder.fav.visibility = if (e.id in favIds) View.VISIBLE else View.GONE
        holder.itemView.isActivated = e.id == playingId
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(items[pos])
        }
        holder.itemView.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onLongClick(items[pos])
            true
        }
    }

    override fun getItemCount() = items.size
}

class PosterAdapter(
    private val onClick: (Entry) -> Unit,
    private val onLongClick: (Entry) -> Unit
) : RecyclerView.Adapter<PosterAdapter.VH>() {

    private var items: List<Entry> = emptyList()
    var favIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivPoster)
        val name: TextView = view.findViewById(R.id.tvName)
        val fav: ImageView = view.findViewById(R.id.ivFav)
    }

    fun submit(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_poster, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.name.text = e.name
        holder.poster.loadImage(e.logo)
        holder.fav.visibility = if (e.id in favIds) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(items[pos])
        }
        holder.itemView.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onLongClick(items[pos])
            true
        }
    }

    override fun getItemCount() = items.size
}

class PlaylistAdapter(
    private val onClick: (Playlist?) -> Unit,
    private val onLongClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.VH>() {

    /** null = بطاقة «إضافة قائمة». */
    private var items: List<Playlist?> = emptyList()
    private var activeId: String? = null

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val name: TextView = view.findViewById(R.id.tvName)
        val type: TextView = view.findViewById(R.id.tvType)
        val active: TextView = view.findViewById(R.id.tvActive)
    }

    fun submit(list: List<Playlist>, activeId: String?) {
        this.items = list + listOf(null)
        this.activeId = activeId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.itemView.context
        if (p == null) {
            holder.icon.setImageResource(R.drawable.ic_add)
            holder.name.text = ctx.getString(R.string.playlists_add)
            holder.type.text = ""
            holder.active.visibility = View.GONE
            holder.itemView.setOnClickListener { onClick(null) }
            holder.itemView.setOnLongClickListener(null)
        } else {
            holder.icon.setImageResource(R.drawable.ic_playlists)
            holder.name.text = p.name
            holder.type.text = when (p.type) {
                PlaylistType.XTREAM -> ctx.getString(R.string.status_xtream)
                PlaylistType.M3U -> ctx.getString(R.string.status_m3u)
                PlaylistType.LOCAL -> ctx.getString(R.string.status_local)
            }
            holder.active.visibility = if (p.id == activeId) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { onClick(p) }
            holder.itemView.setOnLongClickListener { onLongClick(p); true }
        }
    }

    override fun getItemCount() = items.size
}
