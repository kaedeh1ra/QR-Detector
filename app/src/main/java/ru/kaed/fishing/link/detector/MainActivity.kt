package ru.kaed.fishing.link.detector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import ru.kaed.fishing.link.detector.presentation.ScannerScreen
import ru.kaed.fishing.link.detector.presentation.ScannerViewModel
import ru.kaed.fishing.link.detector.ui.theme.FishingLinkDetectorTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FishingLinkDetectorTheme {
                val viewModel: ScannerViewModel = viewModel()
                ScannerScreen(viewModel)
            }
        }
    }
}