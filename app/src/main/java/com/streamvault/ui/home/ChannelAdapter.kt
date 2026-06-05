package com.streamvault.ui.home
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamvault.R
import com.streamvault.data.model.*
class ChannelAdapter(private val list: List<Channel>, private val onClick: (Channel)->Unit) : RecyclerView.Adapter<ChannelAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) { val logo: ImageView=v.findViewById(R.id.imgLogo); val name: TextView=v.findViewById(R.id.tvName); val group: TextView=v.findViewById(R.id.tvGroup) }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_channel, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val ch = list[pos]; h.name.text=ch.name; h.group.text=ch.group?:""
        Glide.with(h.logo).load(ch.logo).placeholder(R.drawable.ic_channel).into(h.logo)
        h.itemView.setOnClickListener { onClick(ch) }
    }
}
class MovieAdapter(private val list: List<Movie>, private val onClick: (Movie)->Unit) : RecyclerView.Adapter<MovieAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) { val cover: ImageView=v.findViewById(R.id.imgCover); val name: TextView=v.findViewById(R.id.tvName) }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_grid, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val m=list[pos]; h.name.text=m.name
        Glide.with(h.cover).load(m.cover).placeholder(R.drawable.ic_movie).into(h.cover)
        h.itemView.setOnClickListener { onClick(m) }
    }
}
class SeriesAdapter(private val list: List<Series>) : RecyclerView.Adapter<SeriesAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) { val cover: ImageView=v.findViewById(R.id.imgCover); val name: TextView=v.findViewById(R.id.tvName) }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_grid, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s=list[pos]; h.name.text=s.name
        Glide.with(h.cover).load(s.cover).placeholder(R.drawable.ic_series).into(h.cover)
    }
}
