package com.koshaq.music.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshaq.music.data.model.PlaylistWithTracks
import com.koshaq.music.databinding.ItemPlaylistBinding


class PlaylistAdapter(
    private val onOpen: (PlaylistWithTracks)->Unit,           // <— НОВЕ
    private val onPlayShuffle: (PlaylistWithTracks)->Unit,
    private val onRename: (PlaylistWithTracks)->Unit,
    private val onDelete: (PlaylistWithTracks)->Unit,
    private val onClear: (PlaylistWithTracks)->Unit,
): ListAdapter<PlaylistWithTracks, PlaylistAdapter.VH>(Diff) {

    object Diff: DiffUtil.ItemCallback<PlaylistWithTracks>(){
        override fun areItemsTheSame(o: PlaylistWithTracks, n: PlaylistWithTracks) =
            o.playlist.playlistId==n.playlist.playlistId
        override fun areContentsTheSame(o: PlaylistWithTracks, n: PlaylistWithTracks) = o==n
    }

    inner class VH(val b: ItemPlaylistBinding): RecyclerView.ViewHolder(b.root){
        fun bind(p: PlaylistWithTracks){
            b.title.text = p.playlist.name
            b.count.text = "${p.tracks.size} tracks"
            b.btnPlayShuffle.setOnClickListener { onPlayShuffle(p) }
            b.btnRename.setOnClickListener { onRename(p) }
            b.btnDelete.setOnClickListener { onDelete(p) }
            b.btnClear.setOnClickListener { onClear(p) }
            b.root.setOnClickListener { onOpen(p) }           // <— НОВЕ
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemPlaylistBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}
