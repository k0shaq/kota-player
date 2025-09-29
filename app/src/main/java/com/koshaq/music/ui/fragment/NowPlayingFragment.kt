package com.koshaq.music.ui.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshaq.music.data.model.TrackEntity
import com.koshaq.music.databinding.FragmentNowPlayingBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel

class NowPlayingFragment : Fragment() {
    private var _vb: FragmentNowPlayingBinding? = null
    private val vb get() = _vb!!
    private val vm: MainViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())


    private val nextAdapter by lazy {
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
            onAddToPlaylistClick = { _ -> }
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return vb.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.upNextList.layoutManager = LinearLayoutManager(requireContext())
        vb.upNextList.adapter = nextAdapter
        vm.controls { player ->
// Bind metadata
            vb.title.text = player.mediaMetadata.title ?: ""
            vb.artist.text = player.mediaMetadata.artist ?: ""
            player.mediaMetadata.artworkData?.let {
                vb.artwork.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
            } ?: run {
// fallback to contentUri image provider if desired
            }


// Seekbar
            vb.seekBar.max = (player.duration.takeIf { it > 0 } ?: 0L).toInt()
            vb.seekBar.progress = player.currentPosition.toInt()
            vb.seekBar.setOnSeekBarChangeListener(object :
                android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    sb: android.widget.SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) vm.controls { it.seekTo(progress.toLong()) }
                }

                override fun onStartTrackingTouch(p0: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(p0: android.widget.SeekBar?) {}
            })


// Controls
            vb.btnPlayPause.setOnClickListener { vm.controls { if (it.isPlaying) it.pause() else it.play() } }
            vb.btnNext.setOnClickListener { vm.controls { it.seekToNext() } }
            vb.btnPrev.setOnClickListener { vm.controls { it.seekToPrevious() } }
            vb.btnShuffle.setOnClickListener {
                vm.controls {
                    it.shuffleModeEnabled = !it.shuffleModeEnabled
                }
            }
            vb.btnRepeat.setOnClickListener {
                vm.controls {
                    it.repeatMode = when (it.repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ONE
                        androidx.media3.common.Player.REPEAT_MODE_ONE -> androidx.media3.common.Player.REPEAT_MODE_ALL
                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                    }
                }
            }


// Up Next list (everything after current index)
            val start = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
            val items = (start until player.mediaItemCount).mapNotNull { idx ->
                val mi = player.getMediaItemAt(idx)
                val md = mi.mediaMetadata
// Build pseudo TrackEntity for display
                TrackEntity(
                    trackId = idx.toLong(),
                    title = md.title?.toString() ?: "",
                    artist = md.artist?.toString() ?: "",
                    album = md.albumTitle?.toString() ?: "",
                    durationMs = 0L,
                    contentUri = mi.localConfiguration?.uri.toString(),
                    dateAdded = 0L
                )
            }
            nextAdapter.submitList(items)


// Update loop for seekbar
            val r = object : Runnable {
                override fun run() {
                    vb.seekBar.max = (player.duration.takeIf { it > 0 } ?: 0L).toInt()
                    vb.seekBar.progress = player.currentPosition.toInt()
                    handler.postDelayed(this, 1000)
                }
            }
            handler.post(r)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView(); handler.removeCallbacksAndMessages(null); _vb = null
    }
}