package com.example.kamlib.presentation.view

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.kamlib.presentation.viewmodel.CameraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CameraPreview(
    context: Context,
    private val textureView: TextureView,
) : LifecycleObserver {
    private val isFrontCamera: Boolean = false
    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var frameCaptureListener: FrameCaptureListener? = null
    private var previewWidth = 512
    private var previewHeight = 512
    private val cameraOpenCloseLock = Semaphore(1)
    val frameCaptureManager = FrameCaptureManager(textureView, previewWidth, previewHeight, context)
    private val viewModel: CameraViewModel =
        ViewModelProvider(context as ViewModelStoreOwner)[CameraViewModel::class.java]
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var isPreviewStopped = false
    private lateinit var imageCapture: ImageCapture

    fun startCameraPreview() {
        if (isPreviewStopped) {
            Log.d("CameraPreview", "Preview is stopped. Not starting again.")
            return
        }
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    if (!isPreviewStopped) {
                        openCamera(width, height)
                    }
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    scaleTextureView(width, height, previewHeight, previewWidth)
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    stopCameraPreview()
                    stopBackgroundThread()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    coroutineScope.launch {
                        viewModel.isCapturingState.collectLatest { isCapturing ->
                            if (isCapturing) {
                                frameCaptureManager.captureFrame()
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(width: Int, height: Int) {
        val cameraId = getCameraId() ?: return
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            val map =
                getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: throw RuntimeException("Cannot get available preview/video sizes")
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    cameraDevice = camera
                    createCameraPreviewSession(map, width, height)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    val errorMsg = when (error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }
                    Log.e(TAG, "Error when trying to connect camera $errorMsg")

                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    fun createCameraPreviewSession(map: StreamConfigurationMap, width: Int, height: Int) {
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
            captureRequestBuilder?.set(
                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_OFF
            )
            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
//                        if (captureRequestBuilder != null) {
//                            session.capture(captureRequestBuilder.build(), null, null)
//                        }
//                        startCameraPreview()

                        if (cameraDevice == null) return
                        cameraCaptureSession = session
                        try {
                            captureRequestBuilder?.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            captureRequestBuilder?.build()
                                ?.let { session.setRepeatingRequest(it, null, backgroundHandler) }

                            // Now that preview is ready, initialize ImageCapture and capture image
//                            imageCapture = ImageCapture(cameraDevice!!, textureView, viewModel)

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
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getCameraId(): String? {
        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (isFrontCamera) {
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        return id
                    }
                } else {
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        return id
                    }
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getCameraCharacteristics(cameraId: String): CameraCharacteristics {
        return cameraManager.getCameraCharacteristics(cameraId)
    }

    private fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int,
        textureViewHeight: Int,
    ): Size {
        val textureViewArea = textureViewWidth * textureViewHeight
        var selectedSize = choices[0]
        var minAreaDiff = Int.MAX_VALUE
        for (size in choices) {
            val area = size.width * size.height
            val areaDiff = abs(area - textureViewArea)
            if (areaDiff < minAreaDiff) {
                selectedSize = size
                minAreaDiff = areaDiff
            }
        }
        return selectedSize
    }

    private fun scaleTextureView(
        textureViewWidth: Int,
        textureViewHeight: Int,
        videoWidth: Int,
        videoHeight: Int,
    ) {
        val matrix = VideoScale.getVideoScaleMatrix(
            textureViewWidth.toFloat(),
            textureViewHeight.toFloat(),
            videoWidth.toFloat(),
            videoHeight.toFloat(),
            VideoScale.CROP_CENTER
        )
        textureView.setTransform(matrix)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun captureImage() {
        coroutineScope.launch {
            delay(1000)
            imageCapture.captureImage()
        }
    }


    private fun handleImageCapture(image: Image) {
        // Convert the image to a Bitmap or save it as needed
        // This is just a placeholder for handling your captured image
        val planes = image.planes
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Here you can convert the byte array to a Bitmap or save it
        // For example, saving to a file (this is just illustrative)
        // saveImageToFile(bytes)
    }


    interface FrameCaptureListener {
        fun onFrameCaptured(frame: Bitmap)
        fun onFramesCaptured(frames: List<Bitmap>)
    }

    fun setFrameCaptureListener(listener: FrameCaptureListener) {
        frameCaptureListener = listener
    }


    fun stopCameraPreview() {
        try {
            isPreviewStopped = true // Set the flag to true when preview is stopped
            cameraOpenCloseLock.acquire()
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            cameraOpenCloseLock.release()
        }
    }
}