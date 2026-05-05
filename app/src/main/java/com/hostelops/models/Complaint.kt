package com.hostelops.models

import com.google.firebase.Timestamp

data class Complaint(
    val id: String = "",
    val studentId: String = "",
    val studentEmail: String = "",
    val category: String = "",
    val description: String = "",
    val location: String = "",
    val photoUrl: String = "",
    val status: String = "NOT_SEEN",
    val timestamp: Timestamp = Timestamp.now(),
    val resolutionPhotoUrl: String = "",
    val staffRemarks: String = "",
    val resolvedAt: Timestamp? = null
)
