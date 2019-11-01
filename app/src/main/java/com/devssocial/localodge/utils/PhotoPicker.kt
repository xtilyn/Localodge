package com.devssocial.localodge.utils

import android.app.Activity
import android.content.Intent


object PhotoPicker {

    fun pickFromGallery(activity: Activity?, requestCode: Int) {
        if (activity == null) return
        //Create an Intent with action as ACTION_PICK
        val intent = Intent(Intent.ACTION_PICK)
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.type = "image/*"
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        // Launching the Intent
        activity.startActivityForResult(intent, requestCode)
    }


}