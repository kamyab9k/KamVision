package com.example.kamvision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kamlib.presentation.view.CameraService
import kotlinx.coroutines.MainScope

class MainActivity : ComponentActivity() {
    private val coroutineScope = MainScope()

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
            } else {
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cameraPermissionGranted = remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                cameraPermissionGranted.value = isCameraPermissionGranted()
                if (!cameraPermissionGranted.value) {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            if (cameraPermissionGranted.value) {
                CameraPreview()
            }
        }
    }

    // Helper method to check if camera permission is granted
    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}

// Composable to show the Camera Preview
@Composable
fun CameraPreview() {
    val coroutineScope = remember { MainScope() }
    var cameraService: CameraService? by remember { mutableStateOf(null) }
    var textureView: TextureView? by remember { mutableStateOf(null) }

    // Use Box to ensure that the preview fills the entire screen
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                // Create the TextureView
                TextureView(ctx).also {
                    textureView = it
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { textureView ->
                if (cameraService == null) {
                    // Initialize the CameraService with the created TextureView
                    cameraService = CameraService.Builder(
                        context = textureView.context,
                        textureView = textureView,
                        scope = coroutineScope
                    ).build()

                    cameraService?.startPreview()
                    cameraService?.captureFrame(20)

                    cameraService!!.getCapturedFrames { frames: List<Bitmap> ->
                        // Handle captured frames here
                        println("Captured frames2: $frames")
                    }
                }
            }
        )
    }

    // Stop the camera preview when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            cameraService?.stopCameraPreview()
        }
    }
}
