package com.koshaq.music.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.koshaq.music.R
import com.koshaq.music.databinding.ActivityMainBinding
import com.koshaq.music.ui.fragment.LibraryFragment
import com.koshaq.music.ui.fragment.NowPlayingFragment
import com.koshaq.music.ui.fragment.PlaylistsFragment
import com.koshaq.music.ui.fragment.QueueFragment
import com.koshaq.music.ui.viewmodel.MainViewModel
import com.koshaq.music.util.ensureAudioPermission


class MainActivity : AppCompatActivity() {
    private lateinit var vb: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        ensureAudioPermission { vm.scanAndPersist() }

        handleDeepLink(intent)

        vb.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library -> swap(LibraryFragment())
                R.id.nav_playlists -> swap(PlaylistsFragment())
                R.id.nav_now -> swap(NowPlayingFragment())
                R.id.nav_queue -> swap(QueueFragment())
            }
            true
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) handleDeepLink(intent)
    }

    private fun handleDeepLink(i: Intent) {
        when (i.getStringExtra("open")) {
            "now" -> swap(com.koshaq.music.ui.fragment.NowPlayingFragment())
            "queue" -> swap(com.koshaq.music.ui.fragment.QueueFragment())
            else -> {
                supportFragmentManager.beginTransaction()
                    .replace(vb.container.id, LibraryFragment())
                    .commit()
            }
        }
    }

    private fun swap(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(vb.container.id, f)
            .commit()
    }
}