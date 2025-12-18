package ru.kaed.fishing.link.detector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.kaed.fishing.link.core.detector.data.repository.UrlSecurityRepositoryImpl
import ru.kaed.fishing.link.core.detector.domain.usecase.ScanUrlUseCase
import ru.kaed.fishing.link.detector.presentation.ScannerScreen
import ru.kaed.fishing.link.detector.presentation.ScannerViewModel
import ru.kaed.fishing.link.detector.ui.theme.FishingLinkDetectorTheme

@Suppress("UNCHECKED_CAST")
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val apiKey = BuildConfig.VIRUSTOTAL_API_KEY

        val repository = UrlSecurityRepositoryImpl(apiKey)
        val scanUseCase = ScanUrlUseCase(repository)

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScannerViewModel(scanUseCase) as T
            }
        }

        setContent {
            FishingLinkDetectorTheme {
                val viewModel: ScannerViewModel = viewModel(factory = viewModelFactory)
                ScannerScreen(viewModel)
            }
        }
    }
}