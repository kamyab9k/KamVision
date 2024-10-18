package com.example.kamlib.presentation.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraViewModel {

    // MutableStateFlow to hold the capturing frames state
    private val _isCapturingFrames = MutableStateFlow(false)
    val isCapturingFrames: StateFlow<Boolean> = _isCapturingFrames

    fun startCapturingFrames(count: Int) {
        // Set the flag to true when capturing frames starts
        _isCapturingFrames.value = true
        // Add any other logic needed for starting to capture frames
    }

    fun stopCapturingFrames() {
        // Set the flag to false when capturing frames stops
        _isCapturingFrames.value = false
    }
}
