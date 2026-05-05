package com.hostelops.models

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "" // STUDENT, STAFF, ADMIN
)
