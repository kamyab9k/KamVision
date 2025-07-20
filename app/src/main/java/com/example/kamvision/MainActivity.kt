package com.example.kamvision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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

@Composable
fun CameraPreview() {
    val coroutineScope =
        rememberCoroutineScope()
    var cameraService: CameraService? by remember { mutableStateOf(null) }
    var textureView: TextureView? by remember { mutableStateOf(null) }

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
                        .setFlashMode(2)
                        .build()
                    cameraService?.startPreview()
                }
                cameraService?.captureFrame(50)
                cameraService?.getCapturedFrames { frames: List<Bitmap> ->
                    println("Hi  $frames")
                }
            }
        )
        Button(
            onClick = {
                coroutineScope.launch {
                    cameraService?.switchCamera()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text("Switch Camera")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraService?.stopCameraPreview()
        }
    }
}