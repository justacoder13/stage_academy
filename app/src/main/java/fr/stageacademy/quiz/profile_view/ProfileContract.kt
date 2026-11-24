package fr.stageacademy.quiz.profile_view

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

internal interface ProfileContract {
    val ready: Boolean
    val saving: Boolean
    val importing: Boolean
    val cropImage: Bitmap?
    val name: String
    val photo: File?
    val nameInvalid: Boolean
    val hasChanges: Boolean
    val message: Int?

    fun load()
    fun editName(value: String)
    fun removePhoto()
    fun importPhoto(uri: Uri)
    fun cancelCrop()
    fun confirmCrop(crop: PhotoCrop)
    fun prepareCapture(): File
    fun acceptCapture(file: File): Boolean
    suspend fun loadPreview(file: File): Bitmap
    fun save()
}
