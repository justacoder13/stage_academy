package fr.stageacademy.quiz.profile_view

internal data class SavedProfile<Photo>(val name: String, val photo: Photo?)

internal interface ProfileRepository<Photo> {
    fun load(): SavedProfile<Photo>?
    fun save(name: String, draftPhoto: Photo?): SavedProfile<Photo>
}

internal interface ProfilePhotos<Source, Preview, Photo> {
    fun prepareCapture(): Photo
    fun isValidCapture(pendingPhoto: Photo): Boolean
    fun previousDraft(): Photo?
    fun discardDraft(photo: Photo?)
    fun loadPreview(photo: Photo): Preview
    fun importPhoto(source: Source): Preview
    fun saveCrop(image: Preview, crop: PhotoCrop): Photo
}
