package com.koshaq.music.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshaq.music.data.model.TrackEntity
import com.koshaq.music.databinding.ItemTrackBinding


class TrackAdapter(
    private val onPlay: (TrackEntity) -> Unit,
    private val onQueue: (TrackEntity) -> Unit,
    private val onAddToPlaylist: (TrackEntity, Long, Int) -> Unit
) : ListAdapter<TrackEntity, TrackAdapter.VH>(Diff) {


    object Diff : DiffUtil.ItemCallback<TrackEntity>() {
        override fun areItemsTheSame(o: TrackEntity, n: TrackEntity) = o.trackId == n.trackId
        override fun areContentsTheSame(o: TrackEntity, n: TrackEntity) = o == n
    }


    inner class VH(val b: ItemTrackBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(t: TrackEntity) {
            b.title.text = t.title
            b.subtitle.text = t.artist
            b.btnPlay.setOnClickListener { onPlay(t) }
            b.btnQueue.setOnClickListener { onQueue(t) }
            b.btnMore.setOnClickListener { /* could show add-to-playlist dialog */ }
        }
    }


    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemTrackBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}