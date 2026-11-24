package fr.stageacademy.quiz.profile_view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

internal data class PhotoCrop(
    val zoom: Float = 1f,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
) {
    fun adjust(image: Bitmap, zoom: Float, panX: Float = 0f, panY: Float = 0f): PhotoCrop {
        val current = bounds(image)
        val next = copy(
            zoom = zoom.coerceIn(1f, MaxZoom),
            centerX = (current.centerX() - panX * current.width()) / image.width,
            centerY = (current.centerY() - panY * current.height()) / image.height,
        )
        val clamped = next.bounds(image)
        return next.copy(centerX = clamped.centerX() / image.width, centerY = clamped.centerY() / image.height)
    }

    fun draw(canvas: Canvas, image: Bitmap, size: Float, paint: Paint) {
        val crop = bounds(image)
        val scale = size / crop.width()
        val destination = RectF(
            -crop.left * scale,
            -crop.top * scale,
            (image.width - crop.left) * scale,
            (image.height - crop.top) * scale,
        )
        canvas.drawBitmap(image, null, destination, paint)
    }

    private fun bounds(image: Bitmap): RectF {
        val side = minOf(image.width, image.height) / zoom
        val left = (centerX * image.width - side / 2f).coerceIn(0f, image.width - side)
        val top = (centerY * image.height - side / 2f).coerceIn(0f, image.height - side)
        return RectF(left, top, left + side, top + side)
    }

    companion object {
        const val MaxZoom = 4f
    }
}
