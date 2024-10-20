package com.example.kamlib.presentation.view

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CameraManagerHelper {


    private fun getCameraId(): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (isFrontCamera) {
                facing == CameraCharacteristics.LENS_FACING_FRONT
            } else {
                facing == CameraCharacteristics.LENS_FACING_BACK
            }
        }
    }


    private fun getCameraCharacteristics(cameraId: String): CameraCharacteristics {
        return cameraManager.getCameraCharacteristics(cameraId)
    }


    private fun openCamera(width: Int, height: Int) {
        scope.launch(Dispatchers.IO) {
            val cameraId = getCameraId() ?: return@launch
            try {
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw RuntimeException("Time out waiting to lock camera opening.")
                }
                val map =
                    getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        ?: throw RuntimeException("Cannot get available preview/video sizes")

                withContext(Dispatchers.Main) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions
                        return@withContext
                    }
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraOpenCloseLock.release()
                            cameraDevice = camera

                            // Launch coroutine to call the suspending function
                            scope.launch {
                                createCameraPreviewSession(map, width, height)
                            }
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            cameraOpenCloseLock.release()
                            camera.close()
                            cameraDevice = null
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            cameraOpenCloseLock.release()
                            camera.close()
                            cameraDevice = null
                        }
                    }, null)
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }




    fun closeCamera() {
        scope.launch(Dispatchers.IO) {
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
    }


}