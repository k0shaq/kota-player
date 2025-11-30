package com.koshaq.music.ui.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshaq.music.data.model.TrackEntity
import com.koshaq.music.databinding.FragmentNowPlayingBinding
import com.koshaq.music.ui.adapter.TrackAdapter
import com.koshaq.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class NowPlayingFragment : Fragment() {

    private var _vb: FragmentNowPlayingBinding? = null
    private val vb get() = _vb!!

    private val vm: MainViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())

    private val nextAdapter by lazy {
        TrackAdapter(
            onPlay = { t ->
                vm.controls { p ->
                    val targetIdx = (0 until p.mediaItemCount).firstOrNull { i ->
                        p.getMediaItemAt(i).localConfiguration?.uri.toString() == t.contentUri
                    }
                    if (targetIdx != null) {
                        p.seekTo(targetIdx, 0)
                        p.playWhenReady = true
                    }
                }
            },
            onQueue = { t ->
                vm.addToQueueNext(t)
            },
            onAddToPlaylistClick = { }
        )
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            vm.controls { p ->
                val dur = p.duration.takeIf { it > 0 } ?: 0L
                val pos = p.currentPosition
                vb.seekBar.max = dur.toInt()
                vb.seekBar.progress = pos.toInt()
                vb.txtElapsed.text = formatTime(pos)
                vb.txtDuration.text = formatTime(dur)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (
                events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_MEDIA_METADATA_CHANGED) ||
                events.contains(Player.EVENT_REPEAT_MODE_CHANGED) ||
                events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                events.contains(Player.EVENT_IS_PLAYING_CHANGED)
            ) {
                bindFromPlayer(player)
            }
        }
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

        vb.btnPlayPause.setOnClickListener {
            vm.controls { p ->
                if (p.playWhenReady) {
                    p.pause()
                } else {
                    p.play()
                }
            }
        }

        vb.btnNext.setOnClickListener { vm.controls { it.seekToNext() } }
        vb.btnPrev.setOnClickListener { vm.smartPrevious() }
        vb.btnRepeat.setOnClickListener { toggleRepeatOne() }
        vb.btnShuffle.setOnClickListener { vm.reshuffleQueuePreserveCurrent() }

        vb.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    vm.controls { it.seekTo(progress.toLong()) }
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
            }
        })

        vb.title.setOnLongClickListener {
            showRenameCurrentTrackDialog()
            true
        }

        vb.artist.setOnLongClickListener {
            showRenameCurrentTrackDialog()
            true
        }

        vm.controls { p ->
            p.addListener(playerListener)
            bindFromPlayer(p)
        }
        handler.post(progressRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        vm.controls { it.removeListener(playerListener) }
        _vb = null
    }

    private fun bindFromPlayer(p: Player) {
        vb.title.text = p.mediaMetadata.title ?: ""
        vb.artist.text = p.mediaMetadata.artist ?: ""

        p.mediaMetadata.artworkData?.let { bytes ->
            vb.artwork.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        } ?: run {
            vb.artwork.setImageDrawable(null)
        }

        updatePlayPauseIcon(p.playWhenReady)

        vb.btnRepeat.alpha = if (p.repeatMode == Player.REPEAT_MODE_ONE) 1f else 0.5f

        val dur = p.duration.takeIf { it > 0 } ?: 0L
        vb.seekBar.max = dur.toInt()
        vb.seekBar.progress = p.currentPosition.toInt()

        val pos = p.currentPosition
        vb.txtElapsed.text = formatTime(pos)
        vb.txtDuration.text = formatTime(dur)

        val start = (p.currentMediaItemIndex + 1).coerceAtMost(p.mediaItemCount)
        val upNext = (start until p.mediaItemCount).map { idx ->
            val mi = p.getMediaItemAt(idx)
            val md = mi.mediaMetadata
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
        nextAdapter.submitList(upNext)
    }

    private fun updatePlayPauseIcon(playWhenReady: Boolean) {
        val icon = if (playWhenReady) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        vb.btnPlayPause.setImageResource(icon)
    }

    private fun toggleRepeatOne() {
        vm.controls { p ->
            p.repeatMode = when (p.repeatMode) {
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_ONE
            }
            vb.btnRepeat.alpha = if (p.repeatMode == Player.REPEAT_MODE_ONE) 1f else 0.5f
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%d:%02d", min, sec)
    }

    private fun showRenameCurrentTrackDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val track = vm.currentItemTrack() ?: return@launch
            val ctx = requireContext()
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 16, 48, 0)
            }
            val inputTitle = EditText(ctx).apply {
                hint = "Назва треку"
                setText(track.title)
            }
            val inputArtist = EditText(ctx).apply {
                hint = "Виконавець"
                setText(track.artist)
            }
            container.addView(inputTitle)
            container.addView(inputArtist)

            MaterialAlertDialogBuilder(ctx)
                .setTitle("Редагувати трек")
                .setView(container)
                .setPositiveButton("Зберегти") { _, _ ->
                    val newTitle = inputTitle.text.toString()
                    val newArtist = inputArtist.text.toString()
                    vm.renameTrack(track.trackId, newTitle, newArtist)
                    vb.title.text = if (newTitle.isNotBlank()) newTitle else track.title
                    vb.artist.text = if (newArtist.isNotBlank()) newArtist else track.artist
                }
                .setNegativeButton("Скасувати", null)
                .show()
        }
    }
}
