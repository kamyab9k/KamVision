package com.example.kamlib.presentation.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

class CameraManagerHelper(
    private val context: Context,
    private val cameraFacing: Int = CameraCharacteristics.LENS_FACING_BACK,
) {
    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private val cameraOpenCloseLock = Semaphore(1)

    suspend fun openCamera(): CameraDevice? =
        withContext(Dispatchers.Main) {
            val cameraId = getCameraId() ?: return@withContext null
            return@withContext suspendCoroutine { continuation ->
                try {
                    if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        throw RuntimeException("Time out waiting to lock camera opening.")
                    }
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Handle permission request here if necessary
                        return@suspendCoroutine
                    }
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraOpenCloseLock.release()
                            cameraDevice = camera
                            continuation.resume(camera)
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            cameraOpenCloseLock.release()
                            camera.close()
                            continuation.resume(null)
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            cameraOpenCloseLock.release()
                            camera.close()
                            continuation.resume(null)
                        }
                    }, Handler(Looper.getMainLooper()))
                } catch (e: CameraAccessException) {
                    continuation.resume(null)
                }
            }
        }

    fun getCameraCharacteristics(cameraId: String): CameraCharacteristics {
        return cameraManager.getCameraCharacteristics(cameraId)
    }

    fun closeCamera() {
        try {
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

    private fun getCameraId(): String? {
        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == cameraFacing) {
                    return id
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun createCameraPreviewSession(
        textureViewSurface: Surface,
        previewSize: Size,
    ): CameraCaptureSession? = withContext(Dispatchers.Main) {
        cameraDevice?.let { camera ->
            return@withContext suspendCoroutine { continuation ->
                try {
                    val captureRequestBuilder =
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(textureViewSurface)
                            set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                        }
                    camera.createCaptureSession(
                        listOf(textureViewSurface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                if (cameraDevice == null) return
                                cameraCaptureSession = session
                                continuation.resume(session)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                continuation.resume(null)
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )
                } catch (e: CameraAccessException) {
                    continuation.resume(null)
                }
            }
        }
        return@withContext null
    }

    fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int,
        textureViewHeight: Int,
    ): Size {
        val textureViewArea = textureViewWidth * textureViewHeight
        return choices.minByOrNull { abs(it.width * it.height - textureViewArea) } ?: choices[0]
    }
}
