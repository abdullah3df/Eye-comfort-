package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.EyeBreakApp
import com.example.ui.EyeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize EyeViewModel utilizing traditional ViewModelProvider
        val viewModel = ViewModelProvider(this)[EyeViewModel::class.java]
        
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
