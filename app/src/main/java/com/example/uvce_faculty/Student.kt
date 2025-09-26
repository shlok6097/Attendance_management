package com.example.uvce_faculty

data class Student(
    val id: String = "",
    val name: String = "",
    val rollNumber: String = "",
    val usn: String = "",
    var status: String = "",
    val attendance: MutableMap<String, String> = mutableMapOf()
)
data class Session(
    val id: String,
    val date: String // formatted date for header display
)
