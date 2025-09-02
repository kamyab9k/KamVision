package com.example.kamvision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kamlib.presentation.view.CameraService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted = mutableStateOf(false)
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            cameraPermissionGranted.value = isGranted
            if (!isGranted) {
                // Optionally, inform the user that permission is needed
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val permissionGranted by remember { cameraPermissionGranted }

            LaunchedEffect(Unit) {
                // Check initial permission status
                cameraPermissionGranted.value = isCameraPermissionGranted()
                if (!cameraPermissionGranted.value) {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            if (permissionGranted) {
                CameraPreview()
            } else {
                // Optional: Show a message to the user while waiting for permission
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission is required to use this app.")
                }
            }
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}


enum class FlashMode(val value: Int) {
    OFF(0),
    ON(1),
    AUTO(2)
}

@Composable
fun CameraPreview() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current // Get the context for the Toast
    var cameraService: CameraService? by remember { mutableStateOf(null) }
    var textureView: TextureView? by remember { mutableStateOf(null) }
    var currentFlashMode by remember { mutableStateOf(FlashMode.OFF) }

    // User-defined number of frames to capture
    val framesToCapture = 50

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also {
                    textureView = it
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { tv ->
                if (cameraService == null) {
                    cameraService = CameraService.Builder(
                        context = tv.context,
                        textureView = tv,
                        scope = coroutineScope
                    )
                        .setFrontCamera(false)
                        .setAutoBrightnessMode(true)
                        .setAutoFocusMode(true)
                        .setFlashMode(currentFlashMode.value)
                        .build()
                    cameraService?.startPreview()
                }
            }
        )

        // Bottom-aligned UI row for flash, capture, and switch buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash button (left side)
            Button(
                onClick = {
                    currentFlashMode = when (currentFlashMode) {
                        FlashMode.OFF -> FlashMode.ON
                        FlashMode.ON -> FlashMode.AUTO
                        FlashMode.AUTO -> FlashMode.OFF
                    }
                    cameraService?.setFlashMode(currentFlashMode.value)
                },
                modifier = Modifier.size(60.dp)
            ) {
                // You can use an icon here
                Text("⚡")
            }

            // Capture Button (middle, larger, with white light fill effect)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)) // Light-filled white circle
                    .border(2.dp, Color.White, CircleShape) // Outer border
                    .clickable {
                        coroutineScope.launch {
                            // Display a toast with the number of frames to capture
                            Toast.makeText(
                                context,
                                "Capturing $framesToCapture frames",
                                Toast.LENGTH_SHORT
                            ).show()

                            cameraService?.captureFrame(framesToCapture)
                            cameraService?.getCapturedFrames { frames: List<Bitmap> ->
                                println("Captured ${frames.size} frames.")
                                // Process captured frames
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Optional: Inner circle for visual effect
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            // Switch Camera Button (right side)
            Button(
                onClick = {
                    coroutineScope.launch {
                        cameraService?.switchCamera()
                    }
                },
                modifier = Modifier.size(60.dp)
            ) {
                // You can use an icon here
                Text("🔄")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraService?.stopCameraPreview()
        }
    }
}