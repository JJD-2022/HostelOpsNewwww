package com.hostelops.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback

object CloudinaryHelper {
    fun init(context: Context) {
        val config = mapOf(
            "cloud_name" to "dfsgc4glg",
            "secure" to true
        )
        try {
            MediaManager.init(context, config)
        } catch (e: Exception) {
            // Already initialized
        }
    }

    fun uploadImage(context: Context, uri: Uri, callback: UploadCallback) {
        MediaManager.get().upload(uri)
            .unsigned("kkkae34a") // Use the same unsigned preset as ComplaintForm
            .option("folder", "profile_pictures")
            .callback(callback)
            .dispatch()
    }
}
