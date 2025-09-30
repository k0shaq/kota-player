package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshaq.music.databinding.FragmentLibraryBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel
import com.koshaq.music.ui.viewmodel.SortBy
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _vb: FragmentLibraryBinding? = null
    private val vb get() = _vb!!
    private val vm: MainViewModel by activityViewModels()

    private lateinit var adapter: TrackAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentLibraryBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = TrackAdapter(
            onPlay = { t ->
                val list = vm.filteredLibrary.value
                val idx = list.indexOfFirst { it.trackId == t.trackId }.coerceAtLeast(0)
                vm.playFromListAt(list, idx, shuffle = false, resetHistory = true)
            },
            onQueue = { t ->
                vm.controls {
                    it.addMediaItem(
                        vm.playerConn.toMediaItem(t.contentUri, t.title, t.artist, t.album)
                    )
                }
            },
            onAddToPlaylistClick = { t -> showAddToPlaylistDialog(t.trackId) }
        )


        vb.list.layoutManager = LinearLayoutManager(requireContext())
        vb.list.adapter = adapter

        vb.inputSearch.doAfterTextChanged { editable ->
            vm.query.value = editable?.toString().orEmpty()
        }

        vb.btnSort.setOnClickListener { showSortDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.filteredLibrary.collect { adapter.submitList(it) }
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(
            "By date added ↑",
            "By date added ↓",
            "By title A→Z",
            "By title Z→A"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Sort by")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> vm.setSort(SortBy.DATE_ADDED_ASC)
                    1 -> vm.setSort(SortBy.DATE_ADDED_DESC)
                    2 -> vm.setSort(SortBy.TITLE_ASC)
                    3 -> vm.setSort(SortBy.TITLE_DESC)
                }
            }
            .show()
    }

    private fun showAddToPlaylistDialog(trackId: Long) {
        lifecycleScope.launch {
            vm.loadPlaylists()
            val lists = vm.playlists.value
            if (lists.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Ще немає плейлистів")
                    .setMessage("Створити перший?")
                    .setPositiveButton("Створити") { _, _ ->
                        promptCreatePlaylist { pid -> vm.addTrackToPlaylist(pid, trackId) }
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            } else {
                val names = lists.map { it.playlist.name }.toTypedArray()
                val ids = lists.map { it.playlist.playlistId }.toLongArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Додати в плейлист")
                    .setItems(names) { _, which ->
                        vm.addTrackToPlaylist(ids[which], trackId)
                    }
                    .setNeutralButton("Новий") { _, _ ->
                        promptCreatePlaylist { pid -> vm.addTrackToPlaylist(pid, trackId) }
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            }
        }
    }

    private fun promptCreatePlaylist(onCreated: (Long) -> Unit) {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Назва плейлиста")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val name = input.text.toString().ifBlank { "Playlist" }
                lifecycleScope.launch {
                    val id = vm.createAndReturnId(name)
                    onCreated(id)
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    override fun onDestroyView() {
        _vb = null
        super.onDestroyView()
    }
}
