package fr.stageacademy.quiz.profile_view

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.stageacademy.quiz.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal class ProfilePresenter(context: Context, private val scope: CoroutineScope) : ProfileContract {
    private val photos = PhotoFiles(context)
    private val store = ProfileStore(context)
    private var saved by mutableStateOf<SavedProfile?>(null)
    private var loading = false

    override var ready by mutableStateOf(false)
        private set
    override var saving by mutableStateOf(false)
        private set
    override var importing by mutableStateOf(false)
        private set
    override var cropImage by mutableStateOf<Bitmap?>(null)
        private set
    override var name by mutableStateOf("")
        private set
    override var photo by mutableStateOf<File?>(null)
        private set
    override var nameInvalid by mutableStateOf(false)
        private set
    override var message by mutableStateOf<Int?>(null)
        private set

    override val hasChanges: Boolean
        get() = name.trim() != saved?.name.orEmpty() || photo != saved?.photo

    override fun load() {
        if (ready || loading) return
        loading = true
        scope.launch {
            try {
                val (profile, draftPhoto) = withContext(Dispatchers.IO) {
                    val profile = store.load()
                    profile to (if (profile == null) photos.previousDraft() else profile.photo)
                }
                saved = profile
                name = profile?.name.orEmpty()
                photo = draftPhoto
            } catch (_: IOException) {
                message = R.string.profile_load_failed
            } finally {
                ready = true
                loading = false
            }
        }
    }

    override fun editName(value: String) {
        if (saving) return
        name = value
        nameInvalid = false
        message = null
    }

    private fun selectPhoto(value: File?) {
        if (photo == value) return
        photos.discardDraft(photo)
        photo = value
        message = null
    }

    override fun prepareCapture(): File = photos.prepareCapture()

    override fun acceptCapture(file: File): Boolean {
        if (saving || importing || !photos.isValidCapture(file)) return false
        selectPhoto(file)
        return true
    }

    override suspend fun loadPreview(file: File) = withContext(Dispatchers.IO) {
        photos.loadPreview(file)
    }

    override fun removePhoto() {
        if (!saving && !importing) selectPhoto(null)
    }

    override fun importPhoto(uri: Uri) {
        if (!ready || saving || importing) return
        importing = true
        message = null
        scope.launch {
            try {
                cropImage = withContext(Dispatchers.IO) { photos.importPhoto(uri) }
            } catch (_: IOException) {
                message = R.string.photo_import_failed
            } catch (_: SecurityException) {
                message = R.string.photo_import_failed
            } finally {
                importing = false
            }
        }
    }

    override fun cancelCrop() {
        if (importing) return
        cropImage = null
        message = null
    }

    override fun confirmCrop(crop: PhotoCrop) {
        val image = cropImage ?: return
        if (importing) return
        importing = true
        message = null
        scope.launch {
            try {
                val draft = withContext(Dispatchers.IO) { photos.saveCrop(image, crop) }
                selectPhoto(draft)
                cropImage = null
            } catch (_: IOException) {
                message = R.string.photo_import_failed
            } finally {
                importing = false
            }
        }
    }

    override fun save() {
        if (!ready || saving || importing || cropImage != null) return
        val trimmedName = name.trim()
        message = null
        if (trimmedName.isBlank()) {
            nameInvalid = true
            return
        }

        val draftPhoto = photo
        saving = true
        scope.launch {
            try {
                val profile = withContext(Dispatchers.IO) { store.save(trimmedName, draftPhoto) }
                saved = profile
                name = profile.name
                photo = profile.photo
                photos.discardDraft(draftPhoto)
                message = R.string.profile_saved
            } catch (_: IOException) {
                message = R.string.profile_save_failed
            } finally {
                saving = false
            }
        }
    }
}
