package com.example.kamlib.presentation.view

import android.content.Context
import android.graphics.Bitmap
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
        get() = textureView.context // Retrieves context from TextureView

    private var imageReader: ImageReader? = null

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

    fun startImageCapture() {
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

        // Configure the image reader listener to process the captured image
        imageReader.setOnImageAvailableListener({ reader ->
            val image: Image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val bitmap = convertImageToBitmap(image)
            image.close()
            viewModel.onImageCaptured(bitmap) // Use ViewModel to handle the captured image
        }, null)

        val rotation = (context.getSystemService(WindowManager::class.java).defaultDisplay.rotation)
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))

        try {
            cameraDevice.createCaptureSession(
                listOf(imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.capture(captureRequestBuilder.build(), null, null)
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
    }

    private fun convertImageToBitmap(image: Image): Bitmap {
        // Your implementation for converting Image to Bitmap
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Convert byte array to Bitmap here (using a suitable method)
        // You might use BitmapFactory or any other suitable method to create a Bitmap from bytes

        return Bitmap.createBitmap(
            512,
            512,
            Bitmap.Config.ARGB_8888
        ) // Replace with your actual bitmap creation
    }
}
