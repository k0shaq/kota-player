package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshaq.music.databinding.FragmentLibraryBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch


class LibraryFragment : Fragment() {

    private var _vb: FragmentLibraryBinding? = null
    private val vb get() = _vb!!
    private val vm: MainViewModel by activityViewModels()

    private val adapter by lazy {
        TrackAdapter(
            onPlay = { t -> vm.playQueueFrom(listOf(t), false) },
            onQueue = { t ->
                vm.controls {
                    it.addMediaItem(vm.playerConn.toMediaItem(t.contentUri, t.title, t.artist, t.album))
                }
            },
            onAddToPlaylistClick = { t -> showAddToPlaylistDialog(t.trackId) }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _vb = FragmentLibraryBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.list.layoutManager = LinearLayoutManager(requireContext())
        vb.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            vm.library.collect { adapter.submitList(it) }
        }

        vb.shuffleAll.setOnClickListener {
            vm.playQueueFrom(adapter.currentList, shuffle = true)
        }
    }

    private fun showAddToPlaylistDialog(trackId: Long) {
        // Один раз завантажимо актуальний список
        viewLifecycleOwner.lifecycleScope.launch {
            vm.loadPlaylists()
            val lists = vm.playlists.value
            if (lists.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Ще немає плейлистів")
                    .setMessage("Створити новий плейлист і додати трек?")
                    .setPositiveButton("Створити") { _, _ ->
                        promptCreatePlaylist { pid -> vm.addTrackToPlaylist(pid, trackId) }
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
                return@launch
            }

            val names = lists.map { it.playlist.name }.toTypedArray()
            val ids = lists.map { it.playlist.playlistId }.toLongArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Додати в плейлист")
                .setItems(names) { _, which ->
                    vm.addTrackToPlaylist(ids[which], trackId)
                }
                .setNegativeButton("Скасувати", null)
                .setNeutralButton("Новий") { _, _ ->
                    promptCreatePlaylist { pid -> vm.addTrackToPlaylist(pid, trackId) }
                }
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
                    val id = vm.createAndReturnId(name)
                    onCreated(id)
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _vb = null
    }
}
