package com.koshaq.music.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.koshaq.music.R
import com.koshaq.music.databinding.ActivityMainBinding
import com.koshaq.music.ui.fragment.LibraryFragment
import com.koshaq.music.ui.fragment.NowPlayingFragment
import com.koshaq.music.ui.fragment.PlaylistsFragment
import com.koshaq.music.ui.fragment.QueueFragment
import com.koshaq.music.ui.fragment.RadioFragment
import com.koshaq.music.ui.viewmodel.MainViewModel
import com.koshaq.music.util.ensureAudioPermission

class MainActivity : AppCompatActivity() {

    private lateinit var vb: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        applySystemInsets()

        ensureAudioPermission { vm.scanAndPersist() }

        handleDeepLink(intent)

        vb.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library -> swap(LibraryFragment())
                R.id.nav_playlists -> swap(PlaylistsFragment())
                R.id.nav_now -> swap(NowPlayingFragment())
                R.id.nav_queue -> swap(QueueFragment())
                R.id.nav_radio -> swap(RadioFragment())
            }
            true
        }

        if (savedInstanceState == null && intent.getStringExtra("open") == null) {
            swap(LibraryFragment())
            vb.bottomNavigation.selectedItemId = R.id.nav_library
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) handleDeepLink(intent)
    }

    private fun handleDeepLink(i: Intent) {
        when (i.getStringExtra("open")) {
            "now" -> {
                swap(NowPlayingFragment())
                vb.bottomNavigation.selectedItemId = R.id.nav_now
            }
            "queue" -> {
                swap(QueueFragment())
                vb.bottomNavigation.selectedItemId = R.id.nav_queue
            }
        }
    }

    private fun swap(f: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(vb.container.id, f)
            .commit()
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(vb.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, 0)
            vb.bottomNavigation.setPadding(
                vb.bottomNavigation.paddingLeft,
                vb.bottomNavigation.paddingTop,
                vb.bottomNavigation.paddingRight,
                sys.bottom
            )
            insets
        }
    }
}
