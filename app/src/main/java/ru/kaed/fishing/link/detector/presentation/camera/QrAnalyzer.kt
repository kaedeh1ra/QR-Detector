package ru.kaed.fishing.link.detector.presentation.camera

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QrAnalyzer(
    private val onQrFound: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @Volatile
    private var isScanning = true

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage == null || !isScanning) {
            imageProxy.close()
            return
        }

        try {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && isScanning) {
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (rawValue != null) {
                                Log.d("QrAnalyzer", "Нашли QR: $rawValue")
                                isScanning = false
                                onQrFound(rawValue)
                                break
                            }
                        }
                    }
                }
                .addOnFailureListener { _ ->
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (_: Exception) {
            imageProxy.close()
        }
    }

    fun resume() {
        isScanning = true
    }
}