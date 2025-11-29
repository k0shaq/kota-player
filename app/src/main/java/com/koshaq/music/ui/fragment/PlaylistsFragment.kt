package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshaq.music.databinding.FragmentPlaylistsBinding
import com.koshaq.music.ui.adapter.PlaylistAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class PlaylistsFragment : Fragment() {
    private var _vb: FragmentPlaylistsBinding? = null
    private val vb get() = _vb!!
    private val vm: MainViewModel by activityViewModels()

    private val adapter = PlaylistAdapter(
        onOpen = { p -> openDetails(p.playlist.playlistId) },
        onPlayShuffle = { p ->
            if (p.tracks.isNotEmpty()) {
                vm.playFromListAt(p.tracks, index = 0, shuffle = true, resetHistory = true)
            }
        },
        onRename = { p -> prompt("Перейменувати", p.playlist.playlistId) },
        onDelete = { p -> confirmDelete(p.playlist.playlistId, p.playlist.name) }
    )

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentPlaylistsBinding.inflate(i, c, false).also { _vb = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.list.layoutManager = LinearLayoutManager(requireContext())
        vb.list.adapter = adapter
        vb.fabAdd.setOnClickListener { prompt("Створити", null) }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.playlists.collect { adapter.submitList(it) }
        }
        vm.loadPlaylists()
    }

    private fun prompt(title: String, id: Long?) {
        val input = EditText(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val t = input.text.toString().ifBlank { "Playlist" }
                if (id == null) vm.createPlaylist(t) else vm.renamePlaylist(id, t)
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun confirmDelete(id: Long, name: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Видалити плейлист?")
            .setMessage("\"$name\" буде видалено назавжди")
            .setPositiveButton("Видалити") { _, _ -> vm.deletePlaylist(id) }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun openDetails(playlistId: Long) {
        parentFragmentManager.beginTransaction()
            .replace(
                (requireView().parent as ViewGroup).id,
                PlaylistDetailsFragment.newInstance(playlistId)
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        _vb = null
        super.onDestroyView()
    }
}
