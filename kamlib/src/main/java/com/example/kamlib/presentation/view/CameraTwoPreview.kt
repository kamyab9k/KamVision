package com.example.kamlib.presentation.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.lifecycle.LifecycleObserver
import com.example.kamlib.presentation.viewmodel.CameraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class CameraTwoPreview(
    private val textureView: TextureView,
    private val viewModel: CameraViewModel,
    private val cameraManagerHelper: CameraManagerHelper,
    private val scope: CoroutineScope, // Use CoroutineScope from ViewModel or Activity
) : LifecycleObserver {
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var isCapturingFrames = false
    private var frameCaptureListener: FrameCaptureListener? = null
    private var previewWidth = 512
    private var previewHeight = 512
    private var imageReader: ImageReader =
        ImageReader.newInstance(previewWidth, previewHeight, ImageFormat.JPEG, 1)

    //TODO: ADD two Formats for frames if possible
    //TODO: optimize coroutines

    init {
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                scope.launch(Dispatchers.IO) { processImage(it) }
            }
            image?.close()
        }, null)
    }

    private suspend fun processImage(image: Image) = withContext(Dispatchers.IO) {
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

    fun startCameraPreview(context: Context, frameCaptureManager: FrameCaptureManager) {
        if (textureView.isAvailable) {
            cameraManagerHelper.openCamera(context, textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    cameraManagerHelper.openCamera(context, width, height)
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    scaleTextureView(width, height, previewHeight, previewWidth)
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    cameraManagerHelper.closeCamera()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    if (isCapturingFrames) {
                        frameCaptureManager.captureFrame()
                    }
                }
            }
        }
    }


    suspend fun createCameraPreviewSession(
        map: StreamConfigurationMap,
        width: Int,
        height: Int,
    ) {
        try {
            val texture = textureView.surfaceTexture
            val previewSize =
                chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height)
            previewWidth = previewSize.width
            previewHeight = previewSize.height
            scaleTextureView(width, height, previewSize.height, previewSize.width)
            texture?.setDefaultBufferSize(previewSize.width, previewHeight)
            val surface = Surface(texture)
            val captureRequestBuilder =
                cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)

            withContext(Dispatchers.Main) {
                cameraDevice?.createCaptureSession(
                    listOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if (cameraDevice == null) return
                            cameraCaptureSession = session
                            try {
                                captureRequestBuilder?.set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )
                                captureRequestBuilder?.build()
                                    ?.let { session.setRepeatingRequest(it, null, null) }
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            // Configuration failed
                        }
                    },
                    null
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int,
        textureViewHeight: Int,
    ): Size {
        val textureViewArea = textureViewWidth * textureViewHeight
        return choices.minByOrNull { size ->
            abs(size.width * size.height - textureViewArea)
        } ?: choices.first()
    }

    private fun scaleTextureView(
        textureViewWidth: Int,
        textureViewHeight: Int,
        videoWidth: Int,
        videoHeight: Int,
    ) {
        val matrix = VideoScaler.getVideoScaleMatrix(
            textureViewWidth.toFloat(),
            textureViewHeight.toFloat(),
            videoWidth.toFloat(),
            videoHeight.toFloat(),
            VideoScaler.CROP_CENTER
        )
        textureView.setTransform(matrix)
    }


    fun stopCameraPreview() {
        scope.launch(Dispatchers.IO) {
            try {
                // Stop capturing frames
                isCapturingFrames = false

                // Close the CameraCaptureSession
                cameraCaptureSession?.close()
                cameraCaptureSession = null

                // Close the CameraDevice
                cameraDevice?.close()
                cameraDevice = null

                // Close the ImageReader to free up resources
                imageReader.close()

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }


    interface FrameCaptureListener {
        fun onFrameCaptured(bitmap: Bitmap)
    }
}
