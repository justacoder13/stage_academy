package fr.stageacademy.quiz.camera_view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class QuizPhotoProvider : FileProvider()

internal class PhotoFiles(private val context: Context) {
    private val directory = File(context.cacheDir, "quiz_photos")
    private val pendingPhoto = File(directory, "pending.jpg")
    private val latestPhoto = File(directory, "latest.jpg")

    fun prepareCapture(): Uri {
        directory.mkdirs()
        pendingPhoto.outputStream().close()
        return FileProvider.getUriForFile(context, "${context.packageName}.quiz.photos", pendingPhoto)
    }

    fun saveCapture(): Boolean {
        if (pendingPhoto.length() == 0L) return false
        Files.move(pendingPhoto.toPath(), latestPhoto.toPath(), REPLACE_EXISTING)
        return true
    }

    fun discardCapture() {
        pendingPhoto.delete()
    }

    fun loadPreview(): Bitmap? {
        if (!latestPhoto.isFile) return null

        val maxSize = 1200
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(latestPhoto)) { decoder, info, _ ->
                val size = info.size
                val largestSide = maxOf(size.width, size.height)
                if (largestSide > maxSize) {
                    val scale = maxSize.toFloat() / largestSide
                    decoder.setTargetSize(
                        (size.width * scale).toInt().coerceAtLeast(1),
                        (size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        }

        val path = latestPhoto.absolutePath
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        val largestSide = maxOf(options.outWidth, options.outHeight)
        var sampleSize = 1
        while (largestSide.toLong() > maxSize.toLong() * sampleSize) sampleSize *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        val bitmap = BitmapFactory.decodeFile(path, options) ?: throw IOException("Invalid photo")

        val orientation = try {
            ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val transform = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        return if (transform.isIdentity) bitmap else {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
        }
    }
}
