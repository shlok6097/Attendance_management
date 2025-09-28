package com.example.uvce_faculty

import com.google.firebase.Timestamp

data class UpcomingTaskItem(
    val id: String = "", // Firestore document ID
    val taskTitle: String = "",
    val subject: String = "",
    val dueDateTime: Timestamp? = null, // Store as Timestamp for easy sorting/display
    // Add other fields if needed for display or interaction, e.g.:
    // val branch: String = "",
    // val semester: String = "",
    // val priority: String = ""
)
