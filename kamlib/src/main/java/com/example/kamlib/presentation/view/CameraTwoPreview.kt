package com.example.kamlib.presentation.view

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleObserver
import com.example.kamlib.presentation.viewmodel.CameraViewModel
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CameraTwoPreview(
    private val context: Context,
    private val textureView: TextureView,
    private val isFrontCamera: Boolean = false,
    private val viewModel: CameraViewModel,
    private val scope: CoroutineScope, // Use CoroutineScope from ViewModel or Activity
) : LifecycleObserver {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var framesCount = 0
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var isCapturingFrames = false
    private var capturedFramesCount = 0
    private val capturedFramesList = mutableListOf<Bitmap>()
    private var frameCaptureListener: FrameCaptureListener? = null
    private var previewWidth = 512
    private var previewHeight = 512
    private val cameraOpenCloseLock = Semaphore(1)
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

    fun startCameraPreview() {
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    openCamera(width, height)
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    scaleTextureView(width, height, previewHeight, previewWidth)
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    closeCamera()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    if (isCapturingFrames) {
                        captureFrame()
                    }
                }
            }
        }
    }

//    private fun openCamera(width: Int, height: Int) {
//        scope.launch(Dispatchers.IO) {
//            val cameraId = getCameraId() ?: return@launch
//            try {
//                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
//                    throw RuntimeException("Time out waiting to lock camera opening.")
//                }
//                val map =
//                    getCameraCharacteristics(cameraId)
//                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//                        ?: throw RuntimeException("Cannot get available preview/video sizes")
//
//                withContext(Dispatchers.Main) {
//                    if (ActivityCompat.checkSelfPermission(
//                            context,
//                            Manifest.permission.CAMERA
//                        ) != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        // TODO: Consider calling
//                        //    ActivityCompat#requestPermissions
//                        // here to request the missing permissions
//                        return@withContext
//                    }
//                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
//                        override fun onOpened(camera: CameraDevice) {
//                            cameraOpenCloseLock.release()
//                            cameraDevice = camera
//
//                            // Launch coroutine to call the suspending function
//                            scope.launch {
//                                createCameraPreviewSession(map, width, height)
//                            }
//                        }
//
//                        override fun onDisconnected(camera: CameraDevice) {
//                            cameraOpenCloseLock.release()
//                            camera.close()
//                            cameraDevice = null
//                        }
//
//                        override fun onError(camera: CameraDevice, error: Int) {
//                            cameraOpenCloseLock.release()
//                            camera.close()
//                            cameraDevice = null
//                        }
//                    }, null)
//                }
//            } catch (e: CameraAccessException) {
//                e.printStackTrace()
//            }
//        }
//    }

    private suspend fun createCameraPreviewSession(
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



//    private fun getCameraCharacteristics(cameraId: String): CameraCharacteristics {
//        return cameraManager.getCameraCharacteristics(cameraId)
//    }

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

    fun stopCapturingFrames() {
        isCapturingFrames = false
    }

//    fun closeCamera() {
//        scope.launch(Dispatchers.IO) {
//            try {
//                cameraOpenCloseLock.acquire()
//                cameraCaptureSession?.close()
//                cameraCaptureSession = null
//                cameraDevice?.close()
//                cameraDevice = null
//            } catch (e: InterruptedException) {
//                throw RuntimeException("Interrupted while trying to lock camera closing.")
//            } finally {
//                cameraOpenCloseLock.release()
//            }
//        }
//    }

//    private fun captureFrame() {
//        if (capturedFramesCount < framesCount) {
//            val bitmap = textureView.bitmap
//            if (bitmap != null) {
//                if (viewModel.resultSuccess.value == true) {
//                    capturedFramesList.add(bitmap.copy(Bitmap.Config.ARGB_8888, false))
//                    capturedFramesCount++
//                }
//            }
//            if (bitmap != null && capturedFramesCount == framesCount) {
//                saveCapturedImage(capturedFramesList.last()) // Save the last captured frame
//                capturedFramesCount = 0 // Reset for the next set of frames
//                capturedFramesList.clear() // Clear the list
//            }
//        }
//    }

//    private fun saveCapturedImage(bitmap: Bitmap) {
//        val filename = "${System.currentTimeMillis()}.jpg"
//        val values = ContentValues().apply {
//            put(MediaStore.Images.Media.TITLE, filename)
//            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
//            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
//        }
//
//        val uri: Uri? =
//            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//
//        uri?.let { imageUri ->
//            try {
//                context.contentResolver.openOutputStream(imageUri).use { outputStream ->
//                    if (outputStream != null) {
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//                    }
//                    Log.d("CameraTwoPreview", "Image saved to gallery: $imageUri")
//                }
//            } catch (e: IOException) {
//                Log.e("CameraTwoPreview", "Error saving image: ${e.message}")
//            }
//        }
//    }

    interface FrameCaptureListener {
        fun onFrameCaptured(bitmap: Bitmap)
    }
}
