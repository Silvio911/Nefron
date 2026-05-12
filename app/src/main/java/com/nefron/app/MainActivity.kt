package com.nefron.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nefron.app.ui.ScheduleScreen
import com.nefron.app.ui.theme.NefronTheme

class MainActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermission.launch(Manifest.permission.READ_CALL_LOG)
        setContent {
            NefronTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScheduleScreen()
                }
            }
        }
    }
}
