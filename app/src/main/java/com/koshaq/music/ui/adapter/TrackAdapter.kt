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
    private val onAddToPlaylistClick: (TrackEntity) -> Unit
) : ListAdapter<TrackEntity, TrackAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<TrackEntity>() {
        override fun areItemsTheSame(oldItem: TrackEntity, newItem: TrackEntity) =
            oldItem.trackId == newItem.trackId

        override fun areContentsTheSame(oldItem: TrackEntity, newItem: TrackEntity) =
            oldItem == newItem
    }

    inner class VH(val b: ItemTrackBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(t: TrackEntity) {
            b.title.text = t.title
            b.subtitle.text = t.artist
            b.btnPlay.setOnClickListener { onPlay(t) }
            b.btnQueue.setOnClickListener { onQueue(t) }
            b.btnMore.setOnClickListener { onAddToPlaylistClick(t) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))
}
