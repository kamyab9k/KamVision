package com.example.kamlib.presentation.view

import android.content.Context
import android.graphics.Bitmap
import android.view.TextureView
import androidx.lifecycle.LifecycleObserver
import kotlinx.coroutines.CoroutineScope


class CameraFacade private constructor(
    private val context: Context,
    private val textureView: TextureView,
    private val scope: CoroutineScope,
    private val resolution: Resolution?,
    private val frameRate: Int?,
) : LifecycleObserver {
    private val frameCaptureManager = FrameCaptureManager(textureView)
    private val cameraPreview: CameraTwoPreview =
        CameraTwoPreview(textureView, scope, resolution, frameRate)

    // Start camera preview
    fun startPreview() {
        cameraPreview.startCameraPreview(context, frameCaptureManager)
    }

    // Capture frame manually
    fun captureFrame() {
        frameCaptureManager.captureFrame()
    }

    // Stop camera preview
    fun stopPreview() {
        cameraPreview.stopCameraPreview()
    }


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

        fun build(): CameraFacade {
            return CameraFacade(
                context = context,
                textureView = textureView,
                scope = scope,
                resolution = resolution,
                frameRate = frameRate,
            )
        }
    }
}

// Helper class to define camera resolution
data class Resolution(val width: Int, val height: Int)

