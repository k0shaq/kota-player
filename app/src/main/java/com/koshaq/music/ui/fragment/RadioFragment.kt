package com.koshaq.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.Player
import com.koshaq.music.databinding.FragmentRadioBinding
import com.koshaq.music.ui.viewmodel.MainViewModel

class RadioFragment : Fragment() {

    private var _vb: FragmentRadioBinding? = null
    private val vb get() = _vb!!

    private val vm: MainViewModel by activityViewModels()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                events.contains(Player.EVENT_IS_PLAYING_CHANGED)
            ) {
                bindState(player)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentRadioBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.btnRadioMain.setOnClickListener {
            vm.toggleRadioMain()
        }
        vb.btnRadioUaRock.setOnClickListener {
            vm.toggleRadioUaRock()
        }

        vm.controls { p ->
            p.addListener(playerListener)
            bindState(p)
        }
    }

    override fun onDestroyView() {
        vm.controls { it.removeListener(playerListener) }
        _vb = null
        super.onDestroyView()
    }

    private fun bindState(player: Player) {
        val uri = player.currentMediaItem?.localConfiguration?.uri?.toString()
        val isPlaying = player.isPlaying

        val isMainPlaying = uri == MainViewModel.RADIO_ROKS_MAIN_URL && isPlaying
        val isUaPlaying = uri == MainViewModel.RADIO_ROKS_UA_ROCK_URL && isPlaying

        vb.btnRadioMain.text =
            if (isMainPlaying) "Зупинити Radio ROKS" else "Увімкнути Radio ROKS"

        vb.btnRadioUaRock.text =
            if (isUaPlaying) "Зупинити Український Рок" else "Увімкнути Український Рок"

        vb.txtStatus.text = when {
            isMainPlaying -> "Зараз грає: Radio ROKS"
            isUaPlaying -> "Зараз грає: Radio ROKS Український рок"
            else -> "Радіо зупинено"
        }
    }
}
