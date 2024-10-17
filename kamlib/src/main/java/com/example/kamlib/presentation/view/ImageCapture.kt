package com.example.kamlib.presentation.view

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.IOException

class ImageCapture (context: Context,bitmap: Bitmap){


     fun saveCapturedImage(context: Context,bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, filename)
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
        }

        val uri: Uri? =
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let { imageUri ->
            try {
                context.contentResolver.openOutputStream(imageUri).use { outputStream ->
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    Log.d("CameraTwoPreview", "Image saved to gallery: $imageUri")
                }
            } catch (e: IOException) {
                Log.e("CameraTwoPreview", "Error saving image: ${e.message}")
            }
        }
    }


}