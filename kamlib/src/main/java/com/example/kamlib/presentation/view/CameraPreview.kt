package com.example.kamlib.presentation.view

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.kamlib.util.VideoScale
import com.example.kamlib.presentation.viewmodel.CameraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CameraPreview(
    context: Context,
    private val textureView: TextureView,
    private val isFrontCamera: Boolean = false,
    private val flashMode: Int?,
    private val autoFocusMode: Int?,
    private val autoBrightnessMode: Int?,
) : DefaultLifecycleObserver { // <-- CHANGE HERE
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
    private var captureJob: Job? = null

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d("CameraPreview", "App Resumed. Starting camera preview.")
        isPreviewStopped = false
        startCameraPreview()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d("CameraPreview", "App Paused. Stopping camera preview and frame capture.")
        stopCameraPreview()
        viewModel.stopCapturing()
    }

    fun startCameraPreview() {
        if (isPreviewStopped) {
            Log.d("CameraPreview", "Preview is stopped. Not starting again.")
            return
        }
        startBackgroundThread()
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
                    // When the view is destroyed, ensure everything is stopped.
                    stopCameraPreview()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    // Only launch this job once to avoid re-launching on every frame.
                    if (captureJob == null || captureJob?.isActive == false) {
                        captureJob = coroutineScope.launch {
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
    }

    private fun startBackgroundThread() {
        // Prevent creating a new thread if one is already running
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").also { it.start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(width: Int, height: Int) {
        // If camera is already open, do nothing
        if (cameraDevice != null) return

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
                        if (cameraDevice == null) return
                        cameraCaptureSession = session
                        try {
                            // Apply Auto Focus Mode
                            autoFocusMode?.let { mode ->
                                captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, mode)
                            }
                            // Apply Auto Brightness Mode
                            autoBrightnessMode?.let { mode ->
                                captureRequestBuilder?.set(CaptureRequest.CONTROL_AE_MODE, mode)
                            }
                            // Apply Flash Mode
                            flashMode?.let { mode ->
                                captureRequestBuilder?.set(CaptureRequest.FLASH_MODE, mode)
                            }

                            captureRequestBuilder?.build()
                                ?.let { session.setRepeatingRequest(it, null, backgroundHandler) }

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
                val desiredFacing = if (isFrontCamera) {
                    CameraCharacteristics.LENS_FACING_FRONT
                } else {
                    CameraCharacteristics.LENS_FACING_BACK
                }
                if (facing == desiredFacing) {
                    return id
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

    private fun handleImageCapture(image: Image) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
    }

    interface FrameCaptureListener {
        fun onFrameCaptured(frame: Bitmap)
        fun onFramesCaptured(frames: List<Bitmap>)
    }

    fun setFrameCaptureListener(listener: FrameCaptureListener) {
        frameCaptureListener = listener
    }

    fun stopCameraPreview() {
        // This check prevents redundant calls
        if (cameraDevice == null && isPreviewStopped) return

        try {
            isPreviewStopped = true
            cameraOpenCloseLock.acquire()
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null
            captureJob?.cancel() // Cancel the frame capture coroutine
            captureJob = null
            stopBackgroundThread()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            cameraOpenCloseLock.release()
        }
    }
}