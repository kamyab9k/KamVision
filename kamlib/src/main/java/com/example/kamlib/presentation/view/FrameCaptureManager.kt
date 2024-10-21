package com.example.kamlib.presentation.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.view.TextureView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.kamlib.presentation.viewmodel.CameraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FrameCaptureManager(
    private val textureView: TextureView,
    private val previewWidth: Int,
    private val previewHeight: Int,
    context: Context,
) {
    private var capturedFramesCount = 0
    private val capturedFramesList = mutableListOf<Bitmap>()
    private var frameCaptureListener: FrameCaptureListener? = null
    private val imageReader: ImageReader =
        ImageReader.newInstance(previewWidth, previewHeight, ImageFormat.JPEG, 1)
    private val viewModel: CameraViewModel =
        ViewModelProvider(context as ViewModelStoreOwner)[CameraViewModel::class.java]

    init {
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let { processImage(it) }
            image?.close()
        }, null)
    }

    private fun processImage(image: Image) {
        val bitmap = image.toBitmap()
        frameCaptureListener?.onFrameCaptured(bitmap)
    }

    private fun Image.toBitmap(): Bitmap {
        val planes = this.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * previewWidth
        val bitmap = Bitmap.createBitmap(
            previewWidth + rowPadding / pixelStride,
            previewHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    fun startCapturingFrames(count: Int) {
        viewModel.setFrameCount(count)
        viewModel.startCapturing() // Call ViewModel to set state
        capturedFramesCount = 0
        capturedFramesList.clear()
    }

    fun captureFrame() {
        if (capturedFramesCount < viewModel.frameCount.value) {
            val bitmap = textureView.bitmap
            if (bitmap != null) {
                capturedFramesList.add(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                capturedFramesCount++
            }
            if (bitmap != null) {
                frameCaptureListener?.onFrameCaptured(bitmap)
            }
            if (viewModel.frameCount.value != 0 && capturedFramesCount == viewModel.frameCount.value) {
                frameCaptureListener?.onFramesCaptured(capturedFramesList)
                viewModel.stopCapturing()
                viewModel.addCapturedFrames(capturedFramesList)
            }
        }
        println("captured frames are $capturedFramesList")
    }

    fun getCapturedFrames(onFramesCaptured: (frames: List<Bitmap>) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val frames = viewModel.getCapturedFrames()
            onFramesCaptured(frames)
        }
    }

    interface FrameCaptureListener {
        fun onFrameCaptured(frame: Bitmap)
        fun onFramesCaptured(frames: List<Bitmap>)
    }

    fun setFrameCaptureListener(listener: FrameCaptureListener) {
        frameCaptureListener = listener
    }
}
