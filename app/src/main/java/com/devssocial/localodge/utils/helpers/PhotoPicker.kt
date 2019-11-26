package com.devssocial.localodge.utils.helpers

import android.graphics.Color
import android.os.Environment
import androidx.fragment.app.Fragment
import com.devssocial.localodge.R
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.features.ReturnMode
import java.net.URLConnection


object PhotoPicker {

    fun isImageFile(path: String?): Boolean {
        val mimeType = URLConnection.guessContentTypeFromName(path)
        return mimeType != null && mimeType.startsWith("image")
    }

    fun isVideoFile(path: String?): Boolean {
        val mimeType: String = URLConnection.guessContentTypeFromName(path)
        return mimeType.startsWith("video")
    }

    fun pickFromGallery(fragment: Fragment, includeVideo: Boolean = false) {
        ImagePicker.create(fragment)
            .theme(R.style.AppTheme)
            .returnMode(ReturnMode.GALLERY_ONLY)
            .folderMode(true) // folder mode (false by default)
            .toolbarFolderTitle("Folders") // folder selection title
            .toolbarImageTitle("Tap to select") // image selection title
            .toolbarArrowColor(Color.BLACK) // Toolbar 'up' arrow color
            .includeVideo(includeVideo) // Show video on image picker
            .single() // single mode
            .showCamera(true) // show camera or not (true by default)
            .imageFullDirectory(Environment.getExternalStorageDirectory().path) // directory name for captured image  ("Camera" folder by default)
            .start()
    }

    fun captureImage(fragment: Fragment?) {
        fragment?.let { ImagePicker.cameraOnly().start(it) }
    }

}