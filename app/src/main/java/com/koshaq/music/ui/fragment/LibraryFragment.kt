package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshaq.music.databinding.FragmentLibraryBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch


class LibraryFragment: Fragment() {
    private var _vb: FragmentLibraryBinding? = null
    private val vb get() = _vb!!
    private val vm: MainViewModel by activityViewModels()
    private val adapter = TrackAdapter(
        onPlay = { t -> vm.playQueueFrom(listOf(t), false) },
        onQueue = { t -> vm.controls { it.addMediaItem(vm.playerConn.toMediaItem(t.contentUri, t.title, t.artist, t.album)) } },
        onAddToPlaylist = { t, pid, pos -> vm.addToPlaylist(pid, t.trackId, pos) }
    )


    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) = FragmentLibraryBinding.inflate(i, c, false).also{_vb=it}.root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.list.layoutManager = LinearLayoutManager(requireContext())
        vb.list.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            vm.library.collect { adapter.submitList(it) }
        }
        vb.shuffleAll.setOnClickListener {
            vm.playQueueFrom(adapter.currentList, true)
        }
    }
    override fun onDestroyView(){ super.onDestroyView(); _vb=null }
}