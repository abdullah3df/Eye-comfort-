package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.EyeBreakApp
import com.example.ui.EyeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startEyeBreakServiceDirectly()
        } else {
            android.util.Log.e("EyeBreakService", "Error: failed to start because POST_NOTIFICATIONS permission was denied")
        }
    }

    fun startEyeBreakServiceSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                startEyeBreakServiceDirectly()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startEyeBreakServiceDirectly()
        }
    }

    private fun startEyeBreakServiceDirectly() {
        val intent = Intent(this, EyeBreakService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            android.util.Log.d("EyeBreakService", "Service started successfully")
        } catch (e: Exception) {
            android.util.Log.e("EyeBreakService", "Error: failed to start", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize EyeViewModel utilizing traditional ViewModelProvider
        val viewModel = ViewModelProvider(this)[EyeViewModel::class.java]
        
        viewModel.onStartServiceRequested = {
            startEyeBreakServiceSafely()
        }

        handleIntent(intent, viewModel)
        
        setContent {
            MyApplicationTheme {
                EyeBreakApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val viewModel = ViewModelProvider(this)[EyeViewModel::class.java]
        handleIntent(intent, viewModel)
    }

    private fun handleIntent(intent: Intent?, viewModel: EyeViewModel) {
        if (intent != null && intent.getBooleanExtra("SHOW_EXERCISE", false)) {
            viewModel.startScreenBreak()
            intent.removeExtra("SHOW_EXERCISE")
            EyeBreakService.resetAlert()
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
