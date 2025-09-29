package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
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

    private val adapter by lazy {
        TrackAdapter(
            onPlay = { t -> vm.playQueueFrom(listOf(t), false) },
            onQueue = { t ->
                vm.controls {
                    it.addMediaItem(vm.playerConn.toMediaItem(t.contentUri, t.title, t.artist, t.album))
                }
            },
            onAddToPlaylistClick = { t -> /* тут можна додавати в інші плейлисти */ }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = requireArguments().getLong(ARG_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _vb = FragmentPlaylistDetailsBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.list.layoutManager = LinearLayoutManager(requireContext())
        vb.list.adapter = adapter

        // завантажуємо назву та треки
        CoroutineScope(Dispatchers.Main).launch {
            val (name, tracks) = loadDetails(playlistId)
            playlistName = name
            vb.title.text = name
            adapter.submitList(tracks)
        }

        vb.btnShuffle.setOnClickListener {
            vm.playQueueFrom(adapter.currentList, shuffle = true)
        }
        vb.btnClear.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Очистити $playlistName?")
                .setMessage("Усі пісні буде прибрано з плейлиста")
                .setPositiveButton("Очистити") { _, _ ->
                    vm.clearPlaylist(playlistId)
                    adapter.submitList(emptyList())
                }
                .setNegativeButton("Скасувати", null)
                .show()
        }
    }

    private suspend fun loadDetails(id: Long): Pair<String, List<TrackEntity>> = withContext(Dispatchers.IO) {
        val dao = com.koshaq.music.data.db.AppDatabase.get(requireContext()).playlistDao()
        val name = dao.getPlaylist(id)?.name ?: "Playlist"
        val tracks = dao.tracksInPlaylist(id)
        name to tracks
    }

    override fun onDestroyView() { super.onDestroyView(); _vb = null }

    companion object {
        private const val ARG_ID = "playlist_id"
        fun newInstance(id: Long) = PlaylistDetailsFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, id) }
        }
    }
}
