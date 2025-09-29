package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            onAddToPlaylistClick = { /* add to playlist */ }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentLibraryBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

    override fun onDestroyView() {
        super.onDestroyView(); _vb = null
    }
}
