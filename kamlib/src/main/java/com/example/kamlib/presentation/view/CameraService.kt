package com.example.kamlib.presentation.view

import android.content.Context
import android.graphics.Bitmap
import android.view.TextureView
import androidx.lifecycle.LifecycleObserver
import kotlinx.coroutines.CoroutineScope


class CameraService private constructor(
    private val context: Context,
    private val textureView: TextureView,
    scope: CoroutineScope,
) : LifecycleObserver {

    //    private val frameCaptureManager = FrameCaptureManager(textureView)
//    private val cameraManagerHelper = CameraManagerHelper(context, scope)
    private val cameraPreview: CameraPreview =
        CameraPreview(context, textureView)
    private val frameCaptureManager: FrameCaptureManager =
        FrameCaptureManager(textureView, 512, 512)


    // Start camera preview API
    fun startPreview() {
        cameraPreview.startCameraPreview()
    }

    // Capture frame manually
    fun captureFrame(frames:Int) {
        frameCaptureManager.startCapturingFrames(frames)
    }

//    // Stop camera preview
//    fun stopPreview() {
//        cameraPreview.stopCameraPreview()
//    }


    // Frame capture listener interface
    interface FrameCaptureListener {
        fun onFrameCaptured(bitmap: Bitmap)
    }

    // Builder class for CameraController to customize behavior
    class Builder(
        private val context: Context,
        private val textureView: TextureView,
        private val scope: CoroutineScope,
    ) {
        private var resolution: Resolution? = null
        private var frameRate: Int? = null
        private var frameCaptureListener: FrameCaptureListener? = null

        fun setResolution(resolution: Resolution) = apply {
            this.resolution = resolution
        }

        fun setFrameRate(frameRate: Int) = apply {
            this.frameRate = frameRate
        }

        fun setFrameCaptureListener(listener: FrameCaptureListener) = apply {
            this.frameCaptureListener = listener
        }

        fun build(): CameraService {
            return CameraService(
                context = context,
                textureView = textureView,
                scope = scope
            )
        }
    }
}

// Helper class to define camera resolution
data class Resolution(val width: Int, val height: Int)

