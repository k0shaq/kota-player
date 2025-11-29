package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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
            onPlay = { t ->
                vm.controls { p ->
                    val targetIdx = (0 until p.mediaItemCount).firstOrNull { i ->
                        p.getMediaItemAt(i).localConfiguration?.uri.toString() == t.contentUri
                    }
                    if (targetIdx != null) {
                        p.seekTo(targetIdx, 0)
                        p.playWhenReady = true
                    }
                }
            },
            onQueue = { },
            onAddToPlaylistClick = { }
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (
                events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_MEDIA_METADATA_CHANGED) ||
                events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) ||
                events.contains(Player.EVENT_REPEAT_MODE_CHANGED)
            ) {
                refresh()
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            refresh()
        }
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

        vb.btnClearQueue.setOnClickListener {
            vm.controls { p ->
                if (p.mediaItemCount > 0) {
                    p.clearMediaItems()
                }
            }
            refresh()
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                vm.controls { p ->
                    if (from in 0 until p.mediaItemCount && to in 0 until p.mediaItemCount) {
                        p.moveMediaItem(from, to)
                    }
                }
                recyclerView.adapter?.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                vm.controls { p ->
                    if (position in 0 until p.mediaItemCount) {
                        p.removeMediaItem(position)
                    }
                }
                refresh()
            }
        })
        touchHelper.attachToRecyclerView(vb.queueList)

        vm.controls { p ->
            p.addListener(playerListener)
        }
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vm.controls { it.removeListener(playerListener) }
        _vb = null
    }

    private fun refresh() {
        vm.controls { p ->
            val items = (0 until p.mediaItemCount).map { idx ->
                val mi = p.getMediaItemAt(idx)
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
}
