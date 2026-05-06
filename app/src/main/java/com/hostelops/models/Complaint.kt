package com.hostelops.models

import com.google.firebase.Timestamp

data class Complaint(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val studentPhone: String = "",
    val category: String = "",
    val description: String = "",
    val roomNo: String = "",
    val block: String = "",
    val photoUrl: String = "",
    val status: String = "NOT_SEEN",
    val timestamp: Timestamp = Timestamp.now(),
    val resolutionPhotoUrl: String = "",
    val staffRemarks: String = "",
    val resolvedAt: Timestamp? = null
)
