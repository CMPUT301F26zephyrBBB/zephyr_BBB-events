package com.example.zephyr_bbb.entrant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// Entrant QR scanner (01.12.01) a screen with a live camera view that reads a campsite's QR code
// and sends the scanned campsite ID to the details screen.
// The camera is shown with CameraX and the code is read with zxing (QrDecoder.kt) so it works
// on any device with a camera and needs no Google Play services as there was issues with this before
// CameraX guide: https://developer.android.com/media/camera/camerax
// Outstanding issues: the mockup has a "Scan" button but this scans continuously so there's no button to press.

/** Message shown when the user has not allowed the camera. */
const val CAMERA_PERMISSION_NEEDED = "Camera access is needed to scan a QR code."

/** Message shown when the camera could not be started. */
const val CAMERA_UNAVAILABLE = "The camera is not available on this device."

/**
 * Composable function that creates the QR scanner screen.
 * It asks for the camera permission when it is opened, then scans until a campsite code is read.
 *
 * @param onBackClick is invoked when the user clicks the back button
 * @param onCampsiteScanned is invoked once, with the campsite ID read from the QR code
 */
@Composable
fun QrScanScreen(
    onBackClick: () -> Unit,
    onCampsiteScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    // Makes sure only one code is acted on, since the camera keeps sending frames
    val finished = remember { AtomicBoolean(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionAsked = true
    }

    if (!hasPermission && !permissionAsked) {
        // Ask as soon as the screen opens
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    QrScanContent(
        hasPermission = hasPermission,
        message = message ?: if (!hasPermission && permissionAsked) CAMERA_PERMISSION_NEEDED else null,
        onBackClick = onBackClick,
        onAllowCameraClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        camera = {
            CameraPreview(
                onText = { text ->
                    val id = campsiteIdFromScan(text)
                    if (id == null) {
                        message = NOT_A_CAMPSITE_CODE
                    } else if (finished.compareAndSet(false, true)) {
                        onCampsiteScanned(id)
                    }
                },
                onError = { message = it }
            )
        }
    )
}

/**
 * Composable function that displays the QR scanner screen.
 * It only shows what it is given, so it can be shown without a camera in tests.
 *
 * @param hasPermission true when the camera may be used, which shows [camera]
 * @param message text to show under the camera, such as "This QR code is not a Zephyr campsite code."
 * @param onBackClick is invoked when the user clicks the back button
 * @param onAllowCameraClick is invoked when the user clicks the button to allow the camera
 * @param camera the camera view, shown only when [hasPermission] is true
 */
@Composable
fun QrScanContent(
    hasPermission: Boolean,
    message: String?,
    onBackClick: () -> Unit,
    onAllowCameraClick: () -> Unit,
    camera: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onBackClick) {
                Text("Back")
            }
            Text(
                text = "Scan QR Code",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Text("Point the camera at a campsite's QR code.")

        if (hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                camera()
            }
        } else {
            Button(
                onClick = onAllowCameraClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow Camera")
            }
        }

        message?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

// Shows the camera and reports the text of each QR code it sees, until the screen is closed
@Composable
private fun CameraPreview(
    onText: (String) -> Unit,
    onError: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // Frames are read on their own thread so the screen stays smooth
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val previewView = PreviewView(context)
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    try {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        val mainExecutor = ContextCompat.getMainExecutor(context)
                        analysis.setAnalyzer(analysisExecutor) { frame ->
                            val text = readFrame(frame)
                            if (text != null) {
                                mainExecutor.execute { onText(text) }
                            }
                        }
                        val camera = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, camera, preview, analysis)
                    } catch (error: Exception) {
                        onError(CAMERA_UNAVAILABLE)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
            previewView
        }
    )
}

// Reads the QR code in one camera frame and always gives the frame back to the camera
private fun readFrame(frame: ImageProxy): String? {
    return try {
        // The first plane of the camera image holds the brightness of each pixel
        val plane = frame.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        decodeQrText(
            luminance = bytes,
            dataWidth = plane.rowStride,
            dataHeight = bytes.size / plane.rowStride,
            width = frame.width,
            height = frame.height
        )
    } finally {
        frame.close()
    }
}
