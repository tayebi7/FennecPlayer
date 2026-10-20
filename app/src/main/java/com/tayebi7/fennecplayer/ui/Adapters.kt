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
    private val onClick: (Entry) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private var items: List<Entry> = emptyList()
    var playingId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.ivLogo)
        val name: TextView = view.findViewById(R.id.tvName)
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
        holder.itemView.isActivated = e.id == playingId
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(items[pos])
        }
    }

    override fun getItemCount() = items.size
}

class PosterAdapter(
    private val onClick: (Entry) -> Unit
) : RecyclerView.Adapter<PosterAdapter.VH>() {

    private var items: List<Entry> = emptyList()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivPoster)
        val name: TextView = view.findViewById(R.id.tvName)
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
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(items[pos])
        }
    }

    override fun getItemCount() = items.size
}
