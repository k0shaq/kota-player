package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.koshaq.music.data.db.AppDatabase
import com.koshaq.music.databinding.FragmentPlaylistDetailsBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistDetailsFragment : Fragment() {

    private var _vb: FragmentPlaylistDetailsBinding? = null
    private val vb get() = _vb!!

    private val vm: MainViewModel by activityViewModels()

    private var playlistId: Long = -1L
    private var playlistName: String = ""

    private val adapter by lazy {
        TrackAdapter(
            onPlay = { t -> vm.playQueueFrom(listOf(t), false) },
            onQueue = { t ->
                vm.controls {
                    it.addMediaItem(
                        vm.playerConn.toMediaItem(
                            t.contentUri,
                            t.title,
                            t.artist,
                            t.album
                        )
                    )
                }
            },
            onAddToPlaylistClick = { /* тут можна додавати в інші плейлисти, якщо потрібно */ }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = requireArguments().getLong(ARG_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentPlaylistDetailsBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.list.layoutManager = LinearLayoutManager(requireContext())
        vb.list.adapter = adapter

        vb.btnShuffle.setOnClickListener {
            vm.playQueueFrom(adapter.currentList, shuffle = true)
        }

        load()

        val touch = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val track = adapter.currentList.getOrNull(vh.bindingAdapterPosition) ?: return
                vm.removeTrackFromPlaylist(playlistId, track.trackId)
                load()
            }
        })
        touch.attachToRecyclerView(vb.list)
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (name, tracks) = withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(requireContext()).playlistDao()
                val n = dao.getPlaylist(playlistId)?.name ?: "Playlist"
                val t = dao.tracksInPlaylist(playlistId)
                n to t
            }
            playlistName = name
            vb.title.text = playlistName
            adapter.submitList(tracks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _vb = null
    }

    companion object {
        private const val ARG_ID = "playlist_id"

        fun newInstance(id: Long) = PlaylistDetailsFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, id) }
        }
    }
}
