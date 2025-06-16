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
    private var isFrontCamera: Boolean,
) : LifecycleObserver {
    private var cameraPreview: CameraPreview =
        CameraPreview(context, textureView, isFrontCamera)
    private val frameCaptureManager: FrameCaptureManager =
        FrameCaptureManager(textureView, 512, 512, context)

    fun startPreview() {
        cameraPreview.startCameraPreview()
    }

    fun stopCameraPreview() {
        cameraPreview.stopCameraPreview()
    }

    fun captureFrame(frames: Int) {
        frameCaptureManager.startCapturingFrames(frames)
    }

    fun getCapturedFrames(onFramesCaptured: (frames: List<Bitmap>) -> Unit) {
        frameCaptureManager.getCapturedFrames(onFramesCaptured)
    }

    fun switchCamera() {
        stopCameraPreview()
        isFrontCamera = !isFrontCamera
        cameraPreview = CameraPreview(context, textureView, isFrontCamera)
        startPreview()
    }

    interface FrameCaptureListener {
        fun onFrameCaptured(bitmap: Bitmap)
    }

    class Builder(
        private val context: Context,
        private val textureView: TextureView,
        private val scope: CoroutineScope,
    ) {
        private var isFrontCamera: Boolean = false

        fun setResolution(resolution: Resolution) = apply {
            // Logic for setting resolution
        }

        fun setFrameRate(frameRate: Int) = apply {
            // Logic for setting frame rate
        }

        fun setFrameCaptureListener(listener: FrameCaptureListener) = apply {
            // Logic for setting frame capture listener
        }

        fun setFrontCamera(isFront: Boolean) = apply {
            this.isFrontCamera = isFront
        }

        fun build(): CameraService {
            return CameraService(
                context = context,
                textureView = textureView,
                scope = scope,
                isFrontCamera = isFrontCamera
            )
        }
    }
}

data class Resolution(val width: Int, val height: Int)