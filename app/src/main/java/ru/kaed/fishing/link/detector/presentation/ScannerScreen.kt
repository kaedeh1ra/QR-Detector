@file:Suppress("DEPRECATION")

package ru.kaed.fishing.link.detector.presentation

import android.content.Intent
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.kaed.fishing.link.detector.core.domain.model.RiskLevel
import ru.kaed.fishing.link.detector.core.domain.model.UrlAnalysisResult
import ru.kaed.fishing.link.detector.presentation.camera.QrAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var qrAnalyzerRef by remember { mutableStateOf<QrAnalyzer?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (state) {
            is ScannerUiState.Success -> {
                showBottomSheet = true
            }
            is ScannerUiState.Idle -> {
                qrAnalyzerRef?.resume()
            }
            else -> {}
        }
    }

    Scaffold { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            CameraPreview(
                onAnalyzerCreated = { analyzer ->
                    qrAnalyzerRef = analyzer
                },
                onQrFound = { url ->
                    Log.d("ScannerScreen", "QR Found: $url")
                    viewModel.onQrScanned(url)
                }
            )

            if (state is ScannerUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state is ScannerUiState.Error) {
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    title = { Text("Ошибка") },
                    text = { Text((state as ScannerUiState.Error).message) },
                    confirmButton = {
                        Button(onClick = { viewModel.resetState() }) { Text("OK") }
                    }
                )
            }
        }

        if (showBottomSheet && state is ScannerUiState.Success) {
            val result = (state as ScannerUiState.Success).result

            ModalBottomSheet(
                onDismissRequest = {
                    viewModel.resetState()
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                ResultBottomSheetContent(
                    result = result,
                    onOpenBrowser = {
                        result.finalUrl?.let { url ->
                            val i = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(i)
                        }
                    },
                    onCopyText = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Scanned QR", result.content)
                        clipboard.setPrimaryClip(clip)
                    }
                )
            }
        }
    }
}

@Composable
fun ResultBottomSheetContent(
    result: UrlAnalysisResult,
    onOpenBrowser: () -> Unit,
    onCopyText: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 50.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, color) = when (result.riskLevel) {
                RiskLevel.SAFE -> Icons.Default.CheckCircle to Color(0xFF4CAF50) // Green
                RiskLevel.DANGEROUS -> Icons.Default.Dangerous to Color(0xFFF44336) // Red
                RiskLevel.SUSPICIOUS -> Icons.Default.Warning to Color(0xFFFF9800) // Orange
                RiskLevel.INFO -> Icons.Default.Info to Color(0xFF2196F3) // Blue
                else -> Icons.AutoMirrored.Filled.Help to Color.Gray
            }
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (result.isUrl) "Анализ завершен" else "Текстовые данные",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Text("Содержимое:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(
            text = result.content,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        if (result.isUrl) {
            Text("Вердикт:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(
                text = result.details,
                color = if (result.riskLevel == RiskLevel.DANGEROUS) Color.Red else Color.Unspecified,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (result.redirectChain.isNotEmpty() && result.redirectChain.size > 1) {
                Text("Цепочка редиректов:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                LazyColumn(
                    modifier = Modifier
                        .height(120.dp) // Ограничиваем высоту списка
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                        .padding(8.dp)
                ) {
                    items(result.redirectChain) { url ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("⬇", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodySmall)
                            Text(url, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCopyText,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Копировать")
            }

            if (result.isUrl) {
                Button(
                    onClick = onOpenBrowser,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (result.riskLevel == RiskLevel.DANGEROUS) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (result.riskLevel == RiskLevel.DANGEROUS) "Открыть (Риск)" else "Открыть")
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onAnalyzerCreated: (QrAnalyzer) -> Unit,
    onQrFound: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)

            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val analyzer = QrAnalyzer(onQrFound)
                onAnalyzerCreated(analyzer)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply { setAnalyzer(Executors.newSingleThreadExecutor(), analyzer) }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

