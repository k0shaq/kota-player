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
import com.koshaq.music.data.db.AppDatabase
import com.koshaq.music.data.model.TrackEntity
import com.koshaq.music.databinding.FragmentPlaylistDetailsBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistDetailsFragment : Fragment() {

    private var _vb: FragmentPlaylistDetailsBinding? = null
    private val vb get() = _vb!!

    private val vm: MainViewModel by activityViewModels()

    private var playlistId: Long = -1L
    private var playlistName: String = ""

    /** Поточний список треків плейлиста (використовується для onPlay) */
    private var currentTracks: List<TrackEntity> = emptyList()

    private val adapter by lazy {
        TrackAdapter(
            onPlay = { t ->
                // знаходимо індекс у повному списку плейлиста та запускаємо з нього
                val idx = currentTracks.indexOfFirst { it.trackId == t.trackId }.coerceAtLeast(0)
                vm.playFromListAt(currentTracks, idx, shuffle = false, resetHistory = true)
            },
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
            onAddToPlaylistClick = { /* за потреби — додавання в інші плейлисти */ }
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

        // Play Shuffle для ВЕСЬ плейлист (з поточного порядку)
        vb.btnShuffle.setOnClickListener {
            if (currentTracks.isNotEmpty()) {
                vm.playFromListAt(currentTracks, index = 0, shuffle = true, resetHistory = true)
            }
        }

        load()

        // Свайп ліворуч/праворуч — видалити трек з плейлиста
        val touch = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
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
        CoroutineScope(Dispatchers.Main).launch {
            val (name, tracks) = withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(requireContext()).playlistDao()
                val n = dao.getPlaylist(playlistId)?.name ?: "Playlist"
                val t = dao.tracksInPlaylist(playlistId).map {
                    // Якщо у DAO повертається entity з іншого пакету — приведи/скопіюй у data.model.TrackEntity за потреби
                    TrackEntity(
                        trackId = it.trackId,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        durationMs = it.durationMs,
                        contentUri = it.contentUri,
                        dateAdded = it.dateAdded
                    )
                }
                n to t
            }
            playlistName = name
            vb.title.text = name
            currentTracks = tracks
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
