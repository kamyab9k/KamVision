package com.example.kamlib.presentation.view

import android.graphics.Bitmap
import android.view.TextureView

class FrameCaptureManager(
    private val textureView: TextureView,
) {
    private var isCapturingFrames = false
    private var framesCount = 0
    private var capturedFramesCount = 0
    private val capturedFramesList = mutableListOf<Bitmap>()

    fun captureFrame() {
        if (capturedFramesCount < framesCount) {
            val bitmap = textureView.bitmap
            if (bitmap != null) {
                if (1==1) {
                    capturedFramesList.add(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                    capturedFramesCount++
                }
            }
            if (bitmap != null && capturedFramesCount == framesCount) {
//                saveCapturedImage(capturedFramesList.last()) // Save the last captured frame
                capturedFramesCount = 0 // Reset for the next set of frames
                capturedFramesList.clear() // Clear the list
            }
        }
    }


    fun stopCapturingFrames() {
        isCapturingFrames = false
    }

}