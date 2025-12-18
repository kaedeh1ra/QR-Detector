@file:Suppress("DEPRECATION")

package ru.kaed.fishing.link.detector.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.kaed.fishing.link.detector.core.domain.model.ChainLink
import ru.kaed.fishing.link.detector.core.domain.model.RiskLevel
import ru.kaed.fishing.link.detector.core.domain.model.UrlAnalysisResult
import ru.kaed.fishing.link.detector.presentation.camera.QrAnalyzer
import java.util.concurrent.Executors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var qrAnalyzerRef by remember { mutableStateOf<QrAnalyzer?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is ScannerUiState.Success) {
            showBottomSheet = true
        }
    }

    Scaffold { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color.Black)) {

            if (hasCameraPermission) {
                CameraPreview(
                    onAnalyzerCreated = { analyzer -> qrAnalyzerRef = analyzer },
                    onQrFound = { url -> viewModel.onQrScanned(url) }
                )

                ScannerOverlay()
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Для работы сканера нужен доступ к камере", color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Дать разрешение")
                    }
                }
            }


            if (state is ScannerUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }


            if (state is ScannerUiState.Error) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.resetState()
                        qrAnalyzerRef?.resume()
                    },
                    title = { Text("Ошибка") },
                    text = { Text((state as ScannerUiState.Error).message) },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.resetState()
                            qrAnalyzerRef?.resume()
                        }) { Text("OK") }
                    }
                )
            }
        }


        if (showBottomSheet && state is ScannerUiState.Success) {
            val result = (state as ScannerUiState.Success).result

            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    viewModel.resetState()
                    qrAnalyzerRef?.resume()
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
fun ScannerOverlay() {
    val boxSize = 250.dp
    val density = LocalDensity.current
    val boxSizePx = with(density) { boxSize.toPx() }
    val cornerRadius = with(density) { 16.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawRect(color = Color.Black.copy(alpha = 0.6f))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(
                    x = (canvasWidth - boxSizePx) / 2,
                    y = (canvasHeight - boxSizePx) / 2
                ),
                size = Size(boxSizePx, boxSizePx),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                blendMode = BlendMode.Clear
            )

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    x = (canvasWidth - boxSizePx) / 2,
                    y = (canvasHeight - boxSizePx) / 2
                ),
                size = Size(boxSizePx, boxSizePx),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Наведите камеру на QR-код",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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

    val (statusColor, statusIcon) = when (result.riskLevel) {
        RiskLevel.SAFE -> Color(0xFF43A047) to Icons.Default.CheckCircle
        RiskLevel.DANGEROUS -> Color(0xFFD32F2F) to Icons.Default.Dangerous
        RiskLevel.SUSPICIOUS -> Color(0xFFF57C00) to Icons.Default.Warning
        RiskLevel.INFO -> Color(0xFF1976D2) to Icons.Default.Info
        else -> Color.Gray to Icons.Default.Help
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 50.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = result.details,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                if (result.isUrl) {
                    Text(
                        text = "Community Score: ${result.communityScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (result.communityScore < 0) Color.Red else Color.Gray
                    )
                }
            }
        }

        if (result.isUrl && result.riskLevel != RiskLevel.SAFE && result.riskLevel != RiskLevel.INFO) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "⚠️ ОБНАРУЖЕНЫ УГРОЗЫ",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("• ${result.maliciousCount} анализаторов пометили сайт как вредоносный")
                    Text("• Оценка репутации пользователей: ${result.communityScore}")
                }
            }
        } else {
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
        }

        if (result.isUrl && result.redirectChain.isNotEmpty()) {
            Text(
                "Путь следования (Редиректы):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(result.redirectChain) { index, link ->
                    RedirectCard(index = index, link = link, isLast = index == result.redirectChain.lastIndex, resultTitle = result.title)
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Text("Содержимое:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(
                text = result.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

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
                Text("Копия")
            }

            if (result.isUrl) {
                Button(
                    onClick = onOpenBrowser,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (result.riskLevel == RiskLevel.DANGEROUS) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (result.riskLevel == RiskLevel.DANGEROUS) "Открыть (Риск!)" else "Открыть сайт")
                }
            }
        }
    }
}

@Composable
fun RedirectCard(index: Int, link: ChainLink, isLast: Boolean, resultTitle: String?) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLast) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))

                Text(
                    text = if (isLast) "Конечный сайт" else link.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLast) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            val displayTitle = if (isLast && !resultTitle.isNullOrEmpty()) {
                resultTitle
            } else {
                try {
                    java.net.URI(link.url).host ?: "Ссылка"
                } catch (e: Exception) { "Ссылка" }
            }

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = link.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
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
                    .setTargetResolution(android.util.Size(1280, 720))
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

