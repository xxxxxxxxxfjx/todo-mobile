package com.focus.tomato

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.focus.tomato.ui.AppRoot
import com.focus.tomato.ui.AppViewModel

class MainActivity : ComponentActivity() {

    private lateinit var appViewModel: AppViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The timer still runs when notifications are denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appViewModel = ViewModelProvider(this)[AppViewModel::class.java]
        requestNotificationPermissionIfNeeded()
        setContent {
            AppRoot(appViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appViewModel.isInitialized) appViewModel.onAppForeground()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
