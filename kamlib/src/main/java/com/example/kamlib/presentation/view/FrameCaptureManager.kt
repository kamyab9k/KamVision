package com.example.kamlib.presentation.view

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.view.TextureView

class FrameCaptureManager(
    private val textureView: TextureView,
    private val previewWidth: Int,
    private val previewHeight: Int,
) {
    private var framesCount = 0
    private var capturedFramesCount = 0
    var isCapturingFrames = false
    private val capturedFramesList = mutableListOf<Bitmap>()
    private var frameCaptureListener: FrameCaptureListener? = null

    private val imageReader: ImageReader =
        ImageReader.newInstance(previewWidth, previewHeight, ImageFormat.JPEG, 1)

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
        framesCount = count
        isCapturingFrames = true
        capturedFramesCount = 0
        capturedFramesList.clear()
    }

    fun captureFrame() {
        if (capturedFramesCount < framesCount) {
            val bitmap = textureView.bitmap
            if (bitmap != null) {
                if (1 == 1) {
                    capturedFramesList.add(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                    capturedFramesCount++
                }
            }
            if (bitmap != null) {
                frameCaptureListener?.onFrameCaptured(bitmap)
            }
            if (framesCount != 0 && capturedFramesCount == framesCount) {
                frameCaptureListener?.onFramesCaptured(capturedFramesList)
                isCapturingFrames = false
            }
        }
        println("captured frames are $capturedFramesList")
    }


    interface FrameCaptureListener {
        fun onFrameCaptured(frame: Bitmap)
        fun onFramesCaptured(frames: List<Bitmap>)
    }

    fun setFrameCaptureListener(listener: FrameCaptureListener) {
        frameCaptureListener = listener
    }

    fun stopCapturingFrames() {
//        isCapturingFrames = false
    }
}
