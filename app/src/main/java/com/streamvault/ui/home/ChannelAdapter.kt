package com.streamvault.ui.home

import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamvault.R
import com.streamvault.data.model.*

class ChannelAdapter(
    private val list: List<Channel>,
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // Construir lista con headers de grupo
    private val items: List<Any> = buildList {
        val grouped = list.groupBy { it.group?.uppercase()?.trim() ?: "GENERAL" }
        grouped.forEach { (group, channels) ->
            add(group) // header
            addAll(channels)
        }
    }

    override fun getItemViewType(pos: Int) = if (items[pos] is String) TYPE_HEADER else TYPE_ITEM
    override fun getItemCount() = items.size

    inner class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tvHeader)
        val tvCount: TextView = v.findViewById(R.id.tvHeaderCount)
    }
    inner class ItemVH(v: View) : RecyclerView.ViewHolder(v) {
        val logo: ImageView = v.findViewById(R.id.imgLogo)
        val name: TextView = v.findViewById(R.id.tvName)
        val group: TextView = v.findViewById(R.id.tvGroup)
        val flag: TextView = v.findViewById(R.id.tvFlag)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): RecyclerView.ViewHolder {
        return if (t == TYPE_HEADER)
            HeaderVH(LayoutInflater.from(p.context).inflate(R.layout.item_group_header, p, false))
        else
            ItemVH(LayoutInflater.from(p.context).inflate(R.layout.item_channel, p, false))
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
        when (h) {
            is HeaderVH -> {
                val group = items[pos] as String
                h.tv.text = group
                // Contar items del grupo
                var count = 0
                for (i in pos + 1 until items.size) {
                    if (items[i] is String) break
                    count++
                }
                h.tvCount.text = "$count canales"
            }
            is ItemVH -> {
                val ch = items[pos] as Channel
                h.name.text = ch.name
                h.group.text = ch.group ?: ""
                h.flag.text = getCountryFlag(ch.name, ch.group)
                Glide.with(h.logo).load(ch.logo).placeholder(R.drawable.ic_channel).into(h.logo)
                h.itemView.setOnClickListener { onClick(ch) }
            }
        }
    }

    private fun getCountryFlag(name: String, group: String?): String {
        val text = "${name.lowercase()} ${group?.lowercase() ?: ""}"
        return when {
            text.contains("usa") || text.contains("united states") || text.contains("us ") -> "🇺🇸"
            text.contains("mexico") || text.contains("mex") || text.contains("mx") -> "🇲🇽"
            text.contains("colombia") || text.contains("col") -> "🇨🇴"
            text.contains("argentina") || text.contains("arg") -> "🇦🇷"
            text.contains("españa") || text.contains("spain") || text.contains("esp") -> "🇪🇸"
            text.contains("chile") || text.contains("chi") -> "🇨🇱"
            text.contains("peru") || text.contains("per") -> "🇵🇪"
            text.contains("venezuela") || text.contains("ven") -> "🇻🇪"
            text.contains("brasil") || text.contains("brazil") || text.contains("bra") -> "🇧🇷"
            text.contains("guatemala") || text.contains("gua") -> "🇬🇹"
            text.contains("uk") || text.contains("england") || text.contains("british") -> "🇬🇧"
            text.contains("france") || text.contains("fra") -> "🇫🇷"
            text.contains("italy") || text.contains("ita") -> "🇮🇹"
            text.contains("germany") || text.contains("ger") -> "🇩🇪"
            text.contains("deportes") || text.contains("sport") || text.contains("dep") -> "⚽"
            text.contains("noticias") || text.contains("news") -> "📰"
            text.contains("infantil") || text.contains("kids") || text.contains("cartoon") -> "🎠"
            text.contains("pelicula") || text.contains("movie") || text.contains("cine") -> "🎬"
            text.contains("musica") || text.contains("music") -> "🎵"
            text.contains("documental") || text.contains("document") -> "🎥"
            else -> "📺"
        }
    }
}

class MovieAdapter(
    private val list: List<Movie>,
    private val onClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ImageView = v.findViewById(R.id.imgCover)
        val name: TextView = v.findViewById(R.id.tvName)
        val rating: TextView = v.findViewById(R.id.tvRating)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_grid, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = list[pos]
        h.name.text = m.name
        h.rating.text = if (!m.rating.isNullOrEmpty()) "⭐ ${m.rating}" else ""
        Glide.with(h.cover).load(m.cover).placeholder(R.drawable.ic_movie).into(h.cover)
        h.itemView.setOnClickListener { onClick(m) }
    }
}

class SeriesAdapter(private val list: List<Series>) : RecyclerView.Adapter<SeriesAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ImageView = v.findViewById(R.id.imgCover)
        val name: TextView = v.findViewById(R.id.tvName)
        val rating: TextView = v.findViewById(R.id.tvRating)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_grid, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = list[pos]
        h.name.text = s.name
        h.rating.text = if (!s.rating.isNullOrEmpty()) "⭐ ${s.rating}" else ""
        Glide.with(h.cover).load(s.cover).placeholder(R.drawable.ic_series).into(h.cover)
    }
}
