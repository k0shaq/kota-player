package com.koshaq.music.ui.fragment

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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


    private val adapter = TrackAdapter(
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
        onAddToPlaylistClick = { t -> showAddToPlaylistDialog(t.trackId) }
    )


    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentLibraryBinding.inflate(i, c, false).also { _vb = it }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.list.layoutManager = LinearLayoutManager(requireContext())
        vb.list.adapter = adapter


        viewLifecycleOwner.lifecycleScope.launch {
            vm.filteredLibrary.collect { adapter.submitList(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.library.collect { setupFilters(it) }
        }


        vb.inputSearch.addTextChangedListener { vm.query.value = it?.toString().orEmpty() }

        vb.shuffleAll.setOnClickListener { vm.playQueueFrom(adapter.currentList, true) }
    }

    private fun setupFilters(tracks: List<TrackEntity>) {
        val ctx = requireContext()
        val artists = mutableListOf("(усі)") + tracks.map { it.artist }.distinct().sorted()
        val albums = mutableListOf("(усі)") + tracks.map { it.album }.distinct().sorted()
        val sorts = listOf("Дата", "Назва", "Артист", "Альбом")


        vb.filterArtist.setAdapter(ArrayAdapter(ctx, R.layout.simple_list_item_1, artists))
        vb.filterAlbum.setAdapter(ArrayAdapter(ctx, R.layout.simple_list_item_1, albums))
        vb.sortBy.setAdapter(ArrayAdapter(ctx, R.layout.simple_list_item_1, sorts))


        vb.filterArtist.setOnItemClickListener { _, _, pos, _ ->
            vm.filterArtist.value = artists[pos].takeIf { it != "(усі)" }
        }
        vb.filterAlbum.setOnItemClickListener { _, _, pos, _ ->
            vm.filterAlbum.value = albums[pos].takeIf { it != "(усі)" }
        }
        vb.sortBy.setOnItemClickListener { _, _, pos, _ ->
            vm.sortBy.value = when (pos) {
                1 -> SortBy.TITLE
                2 -> SortBy.ARTIST
                3 -> SortBy.ALBUM
                else -> SortBy.DATE_ADDED
            }
        }
    }

    private fun showAddToPlaylistDialog(trackId: Long) {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            vm.loadPlaylists()
            val lists = vm.playlists.value
            if (lists.isEmpty()) {
                AlertDialog.Builder(ctx)
                    .setTitle("Ще немає плейлистів")
                    .setMessage("Створити новий і додати трек?")
                    .setPositiveButton("Створити") { _, _ ->
                        promptCreatePlaylist { pid -> vm.addTrackToPlaylist(pid, trackId) }
                    }
                    .setNegativeButton("Скасувати", null)
                    .show(); return@launch
            }
            val names = lists.map { it.playlist.name }.toTypedArray()
            val ids = lists.map { it.playlist.playlistId }.toLongArray()
            AlertDialog.Builder(ctx)
                .setTitle("Додати в плейлист")
                .setItems(names) { _, which -> vm.addTrackToPlaylist(ids[which], trackId) }
                .setNeutralButton("Новий") { _, _ ->
                    promptCreatePlaylist { pid ->
                        vm.addTrackToPlaylist(
                            pid,
                            trackId
                        )
                    }
                }
                .setNegativeButton("Скасувати", null)
                .show()
        }
    }


    private fun promptCreatePlaylist(onCreated: (Long) -> Unit) {
        val input = android.widget.EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Назва плейлиста")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val name = input.text.toString().ifBlank { "Playlist" }
                viewLifecycleOwner.lifecycleScope.launch {
                    val id = vm.createAndReturnId(name); onCreated(id)
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView(); _vb = null
    }
}