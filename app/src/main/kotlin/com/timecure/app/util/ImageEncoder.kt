package com.timecure.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

data class EncodedImage(
    val previewBitmap: Bitmap,
    val dataUrl: String,
    val sizeKb: Int
)

object ImageEncoder {

    /** Creates a temporary FileProvider content URI for full-res camera capture. */
    fun createCameraCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "ai_captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Decodes a gallery or camera image URI, scales it cleanly to at most [maxDimension]px
     * (sharp enough for dense college timetables & handwritten notes while staying compact),
     * and returns both a preview Bitmap and a Base64 JPEG data URL for Vision API calls.
     */
    suspend fun encodeUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024
    ): EncodedImage? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (bounds.outWidth / sample > maxDimension * 2 || bounds.outHeight / sample > maxDimension * 2) {
                sample *= 2
            }

            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            val scaled = scaleDownIfNeeded(decoded, maxDimension)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 78, baos)
            val bytes = baos.toByteArray()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            EncodedImage(
                previewBitmap = scaled,
                dataUrl = "data:image/jpeg;base64,$b64",
                sizeKb = (bytes.size / 1024).coerceAtLeast(1)
            )
        }.getOrNull()
    }

    private fun scaleDownIfNeeded(src: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDim) return src
        val ratio = maxDim.toFloat() / longest.toFloat()
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        val out = Bitmap.createScaledBitmap(src, w, h, true)
        if (out !== src) src.recycle()
        return out
    }
}
