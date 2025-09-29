package com.koshaq.music.util

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment


fun ComponentActivity.ensureAudioPermission(onOk: () -> Unit) {
    val permission =
        if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    val launcher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onOk()
    }
    launcher.launch(permission)
}

fun Fragment.ensureAudioPermission(onOk: () -> Unit) {
    val permission =
        if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    val launcher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onOk()
    }
    launcher.launch(permission)
}