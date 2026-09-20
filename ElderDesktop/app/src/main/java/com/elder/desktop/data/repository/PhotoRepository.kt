package com.elder.desktop.data.repository

import com.elder.desktop.data.local.PhotoStore
import com.elder.desktop.data.model.PhotoItem
import java.io.File

class PhotoRepository(private val store: PhotoStore) {
    fun photos(): List<PhotoItem> = store.listPhotos()
    fun fileFor(photo: PhotoItem): File = store.fileFor(photo)
}
