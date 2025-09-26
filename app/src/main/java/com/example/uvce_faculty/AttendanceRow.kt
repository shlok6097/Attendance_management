package com.example.uvce_faculty

// AttendanceRow.kt (or put in same file if you prefer)
data class AttendanceRow(
    val uid: String,
    val slNo: Int,
    val usn: String,
    val name: String,
    val attendance: MutableMap<String, String> = mutableMapOf()
)

