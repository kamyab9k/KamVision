package com.example.kamlib.presentation.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import com.example.kamlib.presentation.viewmodel.CameraViewModel
import java.nio.ByteBuffer

class ImageCapture(
    private val cameraDevice: CameraDevice,
    private val textureView: TextureView,
    private val viewModel: CameraViewModel,
) {
    private val context: Context
        get() = textureView.context
    private var imageReader: ImageReader? = null
    private val cameraPreview: CameraPreview =
        CameraPreview(context, textureView)

    // Define the ORIENTATIONS mapping
    private val ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }

    init {
        startImageCapture() // Automatically start image capture on initialization
    }

    private fun startImageCapture() {
        // Initialize ImageReader for capturing images
        val width = textureView.width
        val height = textureView.height
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1)
    }

    fun captureImage() {
        val captureRequestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        val imageReader = this.imageReader ?: return

        captureRequestBuilder.addTarget(imageReader.surface)

        val rotation = (context.getSystemService(WindowManager::class.java).defaultDisplay.rotation)
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))

        val texture = textureView.surfaceTexture


        try {
            cameraDevice.createCaptureSession(
                listOf(imageReader.surface,),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.capture(captureRequestBuilder.build(), null, null)
//                        cameraPreview.startCameraPreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("ImageCapture", "Image capture configuration failed.")
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        imageReader.setOnImageAvailableListener({ reader ->
            val image: Image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val bitmap = convertYUV420ToBitmap(image)
            image.close()
            viewModel.onImageCaptured(bitmap)
        }, null)
    }

//    private fun Image.toBitmap(): Bitmap {
//        val planes = this.planes
//        val buffer = planes[0].buffer
//        val pixelStride = planes[0].pixelStride
//        val rowStride = planes[0].rowStride
//        val rowPadding = rowStride - pixelStride * previewWidth
//        val bitmap = Bitmap.createBitmap(
//            previewWidth + rowPadding / pixelStride,
//            previewHeight,
//            Bitmap.Config.ARGB_8888
//        )
//        bitmap.copyPixelsFromBuffer(buffer)
//        return bitmap
//    }

    fun convertYUV420ToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIndex = j * yRowStride + i
                val y = yBuffer.get(yIndex).toInt() and 0xFF

                // U and V are subsampled by a factor of 2 in both dimensions
                val uvIndex = (j / 2) * uvRowStride + (i / 2) * uvPixelStride

                val u = uBuffer.get(uvIndex).toInt() and 0xFF
                val v = vBuffer.get(uvIndex).toInt() and 0xFF

                // Convert YUV to RGB
                val r = (y + 1.370705 * (v - 128)).toInt().coerceIn(0, 255)
                val g = (y - 0.337633 * (u - 128) - 0.698001 * (v - 128)).toInt().coerceIn(0, 255)
                val b = (y + 1.732446 * (u - 128)).toInt().coerceIn(0, 255)

                bitmap.setPixel(i, j, Color.rgb(r, g, b))
            }
        }

        return bitmap
    }
}
