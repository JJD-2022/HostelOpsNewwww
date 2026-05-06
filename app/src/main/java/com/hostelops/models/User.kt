package com.hostelops.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val phone: String = "",
    val rollNo: String = "",
    val address: String = "",
    val block: String = "",
    val roomNo: String = "",
    val photoUrl: String? = null
)
