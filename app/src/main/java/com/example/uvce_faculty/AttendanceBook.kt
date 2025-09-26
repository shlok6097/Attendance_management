package com.example.uvce_faculty

data class AttendanceBook(
    val id: String = "",
    val branch: String = "",
    val semester: String = "",
    val subject: String = "",
    val students: List<String> = emptyList()
)
