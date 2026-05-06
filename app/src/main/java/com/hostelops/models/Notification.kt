package com.hostelops.models

import com.google.firebase.Timestamp

data class Notification(
    val title: String = "",
    val message: String = "",
    val targetRole: String? = null,
    val targetUid: String? = null,
    val timestamp: Timestamp = Timestamp.now()
)
