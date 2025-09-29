package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.koshaq.music.data.model.TrackEntity
import com.koshaq.music.databinding.FragmentQueueBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel

class QueueFragment : Fragment() {
    private var _vb: FragmentQueueBinding? = null
    private val vb get() = _vb!!
    private val vm: MainViewModel by activityViewModels()


    private val adapter by lazy {
        TrackAdapter(
            onPlay = { t -> /* jump to item by matching uri */ vm.controls { c ->
                val idx = (0 until c.mediaItemCount).firstOrNull { i ->
                    c.getMediaItemAt(i).localConfiguration?.uri.toString() == t.contentUri
                } ?: -1
                if (idx >= 0) {
                    c.seekTo(idx, 0); c.play()
                }
            }
            },
            onQueue = { _ -> },
            onAddToPlaylistClick = { _ -> }
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentQueueBinding.inflate(inflater, container, false)
        return vb.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.queueList.layoutManager = LinearLayoutManager(requireContext())
        vb.queueList.adapter = adapter


        refresh()
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = vh.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                vm.controls { it.moveMediaItem(from, to) }
                refresh() // re-read items from controller
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        })
        touchHelper.attachToRecyclerView(vb.queueList)
    }


    private fun refresh() {
        vm.controls { player ->
            val items = (0 until player.mediaItemCount).map { idx ->
                val mi = player.getMediaItemAt(idx)
                val md = mi.mediaMetadata
                TrackEntity(
                    trackId = idx.toLong(),
                    title = md.title?.toString() ?: "",
                    artist = md.artist?.toString() ?: "",
                    album = md.albumTitle?.toString() ?: "",
                    durationMs = 0L,
                    contentUri = mi.localConfiguration?.uri.toString(),
                    dateAdded = 0L
                )
            }
            adapter.submitList(items)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView(); _vb = null
    }
}