package com.example.kamlib.presentation.view

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CaptureRequest
import android.view.TextureView
import androidx.lifecycle.LifecycleOwner
import com.example.kamlib.data.Resolution
import kotlinx.coroutines.CoroutineScope

class CameraService private constructor(
    private val context: Context,
    private val textureView: TextureView,
    scope: CoroutineScope,
    private var isFrontCamera: Boolean,
    private val flashMode: Int?,
    private val autoFocusMode: Int?,
    private val autoBrightnessMode: Int?
) {
    private var cameraPreview: CameraPreview =
        CameraPreview(context, textureView, isFrontCamera, flashMode, autoFocusMode, autoBrightnessMode)
    private val frameCaptureManager: FrameCaptureManager =
        FrameCaptureManager(textureView, 512, 512, context)

    init {
        // Check if the context is a LifecycleOwner (like an Activity) and add an observer.
        // This is the magic that connects your camera to the app's lifecycle.
        (context as? LifecycleOwner)?.lifecycle?.addObserver(cameraPreview)
    }

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
        (context as? LifecycleOwner)?.lifecycle?.removeObserver(cameraPreview)
        stopCameraPreview()
        isFrontCamera = !isFrontCamera
        cameraPreview = CameraPreview(context, textureView, isFrontCamera, flashMode, autoFocusMode, autoBrightnessMode)
        (context as? LifecycleOwner)?.lifecycle?.addObserver(cameraPreview)
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
        private var flashMode: Int? = null
        private var autoFocusMode: Int? = null
        private var autoBrightnessMode: Int? = null

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

        fun setFlashMode(mode: Int) = apply {
            this.flashMode = mode
        }

        fun setAutoFocusMode(enabled: Boolean) = apply {
            this.autoFocusMode = if (enabled) CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE else CaptureRequest.CONTROL_AF_MODE_OFF
        }

        fun setAutoBrightnessMode(enabled: Boolean) = apply {
            this.autoBrightnessMode = if (enabled) CaptureRequest.CONTROL_AE_MODE_ON else CaptureRequest.CONTROL_AE_MODE_OFF
        }

        fun build(): CameraService {
            return CameraService(
                context = context,
                textureView = textureView,
                scope = scope,
                isFrontCamera = isFrontCamera,
                flashMode = flashMode,
                autoFocusMode = autoFocusMode,
                autoBrightnessMode = autoBrightnessMode
            )
        }
    }
}

