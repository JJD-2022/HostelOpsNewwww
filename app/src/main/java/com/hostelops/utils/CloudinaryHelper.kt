package com.hostelops.utils

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryHelper {
    fun init(context: Context) {
        val config = mapOf(
            "cloud_name" to "dfsgc4glg", // TODO: Replace with your Cloudinary Cloud Name
            "secure" to true
        )
        try {
            MediaManager.init(context, config)
        } catch (e: Exception) {
            // Already initialized
        }
    }
}
