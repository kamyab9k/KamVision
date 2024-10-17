package com.example.kamlib.presentation.view

import android.graphics.Bitmap

class FrameCaptureManager {


    private fun captureFrame() {
        if (capturedFramesCount < framesCount) {
            val bitmap = textureView.bitmap
            if (bitmap != null) {
                if (viewModel.resultSuccess.value == true) {
                    capturedFramesList.add(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                    capturedFramesCount++
                }
            }
            if (bitmap != null && capturedFramesCount == framesCount) {
                saveCapturedImage(capturedFramesList.last()) // Save the last captured frame
                capturedFramesCount = 0 // Reset for the next set of frames
                capturedFramesList.clear() // Clear the list
            }
        }
    }





}