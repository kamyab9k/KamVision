package com.example.kamvision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kamlib.presentation.view.CameraService
import kotlinx.coroutines.MainScope

class MainActivity : ComponentActivity() {

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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
                    cameraService = CameraService.Builder(
                        context = textureView.context,
                        textureView = textureView,
                        scope = coroutineScope
                    ).build()
                    cameraService?.startPreview()

                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraService?.stopCameraPreview()
        }
    }
}
