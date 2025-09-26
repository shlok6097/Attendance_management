package com.example.uvce_faculty


data class AttendanceRow(
    val uid: String,
    val slNo: Int,
    val usn: String,
    val name: String,
    val attendance: MutableMap<String, String> = mutableMapOf()
)

