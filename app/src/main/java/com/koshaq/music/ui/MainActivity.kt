package com.koshaq.music.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.koshaq.music.R
import com.koshaq.music.databinding.ActivityMainBinding
import com.koshaq.music.ui.fragment.LibraryFragment
import com.koshaq.music.ui.fragment.PlaylistsFragment
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

        supportFragmentManager.beginTransaction()
            .replace(vb.container.id, LibraryFragment())
            .commit()

        vb.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library ->
                    supportFragmentManager.beginTransaction()
                        .replace(vb.container.id, LibraryFragment())
                        .commit()

                R.id.nav_playlists ->
                    supportFragmentManager.beginTransaction()
                        .replace(vb.container.id, PlaylistsFragment())
                        .commit()
            }
            true
        }
    }
}