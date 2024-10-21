package com.example.kamlib.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraViewModel : ViewModel() {
    // StateFlow to track the capturing state
    private val _isCapturingState = MutableStateFlow(false)
    val isCapturingState: StateFlow<Boolean> get() = _isCapturingState

    private var _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> get() = _frameCount

    private val _capturedFrames = MutableStateFlow<MutableList<Bitmap>>(mutableListOf())
    val capturedFrames: StateFlow<MutableList<Bitmap>> get() = _capturedFrames

    fun startCapturing() {
        _isCapturingState.value = true // Set to true when starting to capture
    }

    fun stopCapturing() {
        _isCapturingState.value = false // Set to false when stopping capturing
    }

    fun setFrameCount(count: Int) {
        _frameCount.value = count // Set to false when stopping capturing
    }

    fun getFrameCount() {
        frameCount
    }

    fun addCapturedFrames(frames: List<Bitmap>) {
        _capturedFrames.value = _capturedFrames.value.apply {
            addAll(frames) // Add the list of frames to the current list
        }
    }

    // Retrieve the captured frames list
    fun getCapturedFrames(): MutableList<Bitmap> {
        return _capturedFrames.value
    }

    fun clearCapturedFrames() {
        _capturedFrames.value.clear()
    }

}