package com.dairypulse.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Cow and farm photos. A picked or captured image is shrunk, turned upright and saved as a
 * JPEG in the app's private "photos" folder (which Android Auto Backup also copies).
 * Records keep only the file name.
 */
object Photos {
    /** Longest side of a saved cow photo, in pixels. Keeps each file around 100–200 KB. */
    const val COW_SIDE = 1080

    /** Longest side of the saved farm cover photo. */
    const val COVER_SIDE = 1600

    private const val DIR = "photos"

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    /** A fresh content URI for the camera app to write a new picture into. */
    fun newCaptureUri(context: Context): Uri {
        val folder = File(context.cacheDir, "camera").apply { mkdirs() }
        val target = File(folder, "capture.jpg").apply { delete() }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    }

    /** Copies the image at [uri] into the photos folder. Returns the new file name, or null if it can't be read. */
    suspend fun import(context: Context, uri: Uri, maxSide: Int): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            val rotation = resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val scale = minOf(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
            val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
            val upright = if (scale < 1f || rotation != 0f) {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else decoded

            val name = "p_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            file(context, name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            if (upright !== decoded) decoded.recycle()
            name
        }.getOrNull()
    }

    fun delete(context: Context, name: String?) {
        if (name != null) runCatching { file(context, name).delete() }
    }

    /** Removes photo files no record points to (e.g. picked, then the form was left without saving). */
    fun deleteUnused(context: Context, inUse: Set<String>) {
        dir(context).listFiles()?.filter { it.name !in inUse }?.forEach { it.delete() }
    }

    // Decoded bitmaps, keyed by file name and requested size. Sized in kilobytes.
    private val cache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(8 * 1024),
    ) {
        override fun sizeOf(key: String, value: ImageBitmap) = (value.width * value.height * 4 / 1024).coerceAtLeast(1)
    }

    fun cached(name: String, maxPx: Int): ImageBitmap? = cache.get("$name@$maxPx")

    /** Decodes a saved photo no larger than about [maxPx] on its longest side. */
    suspend fun load(context: Context, name: String, maxPx: Int): ImageBitmap? {
        cached(name, maxPx)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val f = file(context, name)
                if (!f.exists()) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, bounds)
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
                BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?.asImageBitmap()?.also { cache.put("$name@$maxPx", it) }
            }.getOrNull()
        }
    }
}
