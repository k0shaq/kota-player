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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshaq.music.data.db.AppDatabase
import com.koshaq.music.data.model.TrackEntity
import com.koshaq.music.databinding.FragmentPlaylistDetailsBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel
import com.koshaq.music.ui.viewmodel.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistDetailsFragment : Fragment() {

    private var _vb: FragmentPlaylistDetailsBinding? = null
    private val vb get() = _vb!!

    private val vm: MainViewModel by activityViewModels()

    private var playlistId: Long = -1L
    private var playlistName: String = ""
    private var currentTracks: List<TrackEntity> = emptyList()
    private var currentSort: SortBy? = SortBy.DATE_ADDED_DESC

    private val adapter: TrackAdapter by lazy {
        TrackAdapter(
            onPlay = { t ->
                val list = adapter.currentList
                val idx = list.indexOfFirst { it.trackId == t.trackId }.coerceAtLeast(0)
                vm.playFromListAt(list, idx, shuffle = false, resetHistory = true)
            },
            onQueue = { t ->
                vm.addToQueueNext(t)
            },
            onAddToPlaylistClick = { }
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
            if (adapter.currentList.isNotEmpty()) {
                vm.playFromListAt(
                    adapter.currentList,
                    index = 0,
                    shuffle = true,
                    resetHistory = true
                )
            }
        }

        vb.btnQuickAdd.setOnClickListener {
            showQuickAddDialog()
        }

        vb.btnSort.setOnClickListener {
            showSortDialog()
        }

        load()

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
        viewLifecycleOwner.lifecycleScope.launch {
            val (name, tracks) = withContext(Dispatchers.IO) {
                val db = AppDatabase.get(requireContext())
                val dao = db.playlistDao()
                val n = dao.getPlaylist(playlistId)?.name ?: "Playlist"
                val t = dao.tracksInPlaylist(playlistId).map {
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
            currentTracks = tracks
            vb.title.text = name
            vb.stats.text = formatStats(tracks)
            applySortAndSubmit(scrollToTop = false)
        }
    }

    private fun applySortAndSubmit(scrollToTop: Boolean) {
        val sorted = if (currentSort != null) {
            vm.sortList(currentTracks, currentSort!!)
        } else {
            currentTracks
        }
        adapter.submitList(sorted) {
            if (scrollToTop) {
                vb.list.scrollToPosition(0)
            }
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(
            "Власна черга",
            "За датою додавання ↑",
            "За датою додавання ↓",
            "За назвою A→Я",
            "За назвою Я→A"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Сортувати плейлист")
            .setItems(options) { _, which ->
                currentSort = when (which) {
                    0 -> null
                    1 -> SortBy.DATE_ADDED_ASC
                    2 -> SortBy.DATE_ADDED_DESC
                    3 -> SortBy.TITLE_ASC
                    4 -> SortBy.TITLE_DESC
                    else -> null
                }
                applySortAndSubmit(scrollToTop = true)
            }
            .show()
    }

    private fun formatStats(tracks: List<TrackEntity>): String {
        if (tracks.isEmpty()) return "0 треків"
        val count = tracks.size
        val totalMs = tracks.sumOf { it.durationMs }
        val totalSec = totalMs / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val tracksPart = "$count треків"
        val durationPart = if (hours > 0) {
            "${hours} год ${minutes} хв"
        } else {
            "${minutes} хв"
        }
        return "$tracksPart • $durationPart"
    }

    private fun showQuickAddDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                val db = AppDatabase.get(requireContext())
                val playlistDao = db.playlistDao()
                val trackDao = db.trackDao()

                val existing = playlistDao.tracksInPlaylist(playlistId)
                val existingIds = existing.map { it.trackId }.toSet()

                trackDao.all().filter { it.trackId !in existingIds }
            }

            if (candidates.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Немає нових треків")
                    .setMessage("Усі доступні треки вже є в цьому плейлисті")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val titles = candidates.map { t ->
                val artist = if (t.artist.isBlank()) "Unknown" else t.artist
                "${t.title} — $artist"
            }.toTypedArray()

            val checked = BooleanArray(candidates.size)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Швидке наповнення")
                .setMultiChoiceItems(titles, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("Додати") { _, _ ->
                    val toAdd = candidates.indices
                        .filter { checked[it] }
                        .map { candidates[it] }

                    if (toAdd.isNotEmpty()) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            toAdd.forEach { track ->
                                vm.addTrackToPlaylist(playlistId, track.trackId)
                            }
                            load()
                        }
                    }
                }
                .setNegativeButton("Скасувати", null)
                .show()
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