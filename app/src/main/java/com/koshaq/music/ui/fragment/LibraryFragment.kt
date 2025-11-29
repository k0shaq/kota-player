package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshaq.music.data.model.TrackEntity
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

    private var scrollToTopOnNextList = false

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
                vm.addToQueueNext(t)
            },
            onAddToPlaylistClick = { t ->
                showTrackMenu(t)
            }
        )

        vb.list.layoutManager = LinearLayoutManager(requireContext())
        vb.list.adapter = adapter

        vb.inputSearch.doAfterTextChanged { editable ->
            vm.query.value = editable?.toString().orEmpty()
        }

        vb.btnSort.setOnClickListener { showSortDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.filteredLibrary.collect { list ->
                adapter.submitList(list) {
                    if (scrollToTopOnNextList) {
                        vb.list.scrollToPosition(0)
                        scrollToTopOnNextList = false
                    }
                }
            }
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(
            "За датою додавання ↑",
            "За датою додавання ↓",
            "За назвою A→Я",
            "За назвою Я→A"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Сортувати")
            .setItems(options) { _, which ->
                scrollToTopOnNextList = true
                when (which) {
                    0 -> vm.setSort(SortBy.DATE_ADDED_ASC)
                    1 -> vm.setSort(SortBy.DATE_ADDED_DESC)
                    2 -> vm.setSort(SortBy.TITLE_ASC)
                    3 -> vm.setSort(SortBy.TITLE_DESC)
                }
            }
            .show()
    }

    private fun showTrackMenu(track: TrackEntity) {
        val options = arrayOf(
            "Додати в плейлист",
            "Видалити з пристрою"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(track.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddToPlaylistDialog(track.trackId)
                    1 -> confirmDeleteTrack(track)
                }
            }
            .show()
    }

    private fun confirmDeleteTrack(track: TrackEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Видалити трек?")
            .setMessage("«${track.title}» буде видалено з пристрою")
            .setPositiveButton("Видалити") { _, _ ->
                vm.deleteTrackLegacy(track)
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun showAddToPlaylistDialog(trackId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.loadPlaylists()
            val lists = vm.playlists.value
            if (lists.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Ще немає плейлистів")
                    .setMessage("Створити перший?")
                    .setPositiveButton("Створити") { _, _ ->
                        promptCreatePlaylist { pid ->
                            vm.addTrackToPlaylist(pid, trackId)
                        }
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            } else {
                val names = lists.map { it.playlist.name }.toTypedArray()
                val ids = lists.map { it.playlist.playlistId }.toLongArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Додати в плейлист")
                    .setItems(names) { _, which ->
                        vm.addTrackToPlaylist(ids[which], trackId)
                    }
                    .setNeutralButton("Новий") { _, _ ->
                        promptCreatePlaylist { pid ->
                            vm.addTrackToPlaylist(pid, trackId)
                        }
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            }
        }
    }

    private fun promptCreatePlaylist(onCreated: (Long) -> Unit) {
        val input = EditText(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Назва плейлиста")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val name = input.text.toString().ifBlank { "Playlist" }
                viewLifecycleOwner.lifecycleScope.launch {
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
