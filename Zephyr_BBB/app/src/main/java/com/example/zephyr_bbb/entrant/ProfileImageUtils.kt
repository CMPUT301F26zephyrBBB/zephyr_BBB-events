package com.example.zephyr_bbb.entrant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Shrinks a picked photo before it's uploaded as a profile image.
 * Phone photos are often several MB, and Storage only accepts images up to 2 MB,
 * so the photo is scaled down to at most [MAX_DIMENSION] px and saved as a JPEG.
 * US 01.08.02.
 */
object ProfileImageUtils {

    /** Longest side of an uploaded profile photo, in pixels. */
    const val MAX_DIMENSION = 512

    /** JPEG quality used for uploaded profile photos. */
    const val JPEG_QUALITY = 85

    /**
     * The biggest power of two that can be passed to [BitmapFactory.Options.inSampleSize]
     * while keeping both sides at least [maxDim], so the photo is decoded small
     * without losing quality we need.
     *
     * @param width the original image width in pixels
     * @param height the original image height in pixels
     * @param maxDim the longest side we want to end up with
     * @return 1 if the image is already small, otherwise 2, 4, 8, ...
     */
    fun sampleSizeFor(width: Int, height: Int, maxDim: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxDim && height / (sampleSize * 2) >= maxDim) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * The size to scale an image to so its longest side is at most [maxDim],
     * keeping its shape. Images that already fit are left alone.
     *
     * @param width the image width in pixels
     * @param height the image height in pixels
     * @param maxDim the longest side allowed
     * @return the target (width, height), each at least 1
     */
    fun scaledSize(width: Int, height: Int, maxDim: Int): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxDim) return width to height
        val scale = maxDim.toFloat() / longest
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    /**
     * Decodes the photo at [uri], turns it upright using its EXIF orientation,
     * shrinks it, and writes it to a JPEG in the cache folder.
     * Does file work, so call it off the main thread.
     *
     * @param context used to open the photo and find the cache folder
     * @param uri the photo picked by the user
     * @param maxDim the longest side of the saved JPEG
     * @return the JPEG file, which the caller should delete after uploading
     * @throws IOException if the photo can't be read or decoded
     */
    fun downscaleToJpeg(context: Context, uri: Uri, maxDim: Int = MAX_DIMENSION): File {
        val resolver = context.contentResolver

        // First pass only reads the size, so a huge photo is never fully loaded
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("That file isn't an image we can read.")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDim)
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("That file isn't an image we can read.")

        val (targetWidth, targetHeight) = scaledSize(decoded.width, decoded.height, maxDim)
        val scaled = decoded.scale(targetWidth, targetHeight)
        val upright = rotateUpright(scaled, readRotation(context, uri))

        val file = File.createTempFile("profile_", ".jpg", context.cacheDir)
        file.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return file
    }

    // camera photos are often stored sideways with an EXIF tag saying how to turn them
    private fun readRotation(context: Context, uri: Uri): Int {
        // a photo with no readable EXIF data is treated as already upright
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: IOException) {
            null
        } ?: ExifInterface.ORIENTATION_NORMAL
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotateUpright(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
