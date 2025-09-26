package com.example.uvce_faculty

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class AttendanceSheetFragment : Fragment() {

    private lateinit var bookId: String
    private lateinit var db: FirebaseFirestore
    private lateinit var webView: WebView

    private val studentList = mutableListOf<Student>()
    private val sessionList = mutableListOf<Session>()
    private val inProgressCheckIns = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookId = arguments?.getString("bookId") ?: ""
        db = FirebaseFirestore.getInstance()
        Log.d("AttendanceSheet", "Fragment created. bookId=$bookId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_attendance_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        webView = view.findViewById(R.id.attendanceWebView)
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        // Attach JS interface
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        webView.loadUrl("file:///android_asset/attendance.html")
        fetchSessionsAndStudents()
    }

    /** ------------------- JS Bridge ------------------- **/
    inner class WebAppInterface {
        @JavascriptInterface
        fun updateAttendance(studentId: String, sessionId: String, status: String) {
            markAttendance(studentId, sessionId, status)
        }
    }

    /** ------------------- Fetch Sessions & Students ------------------- **/
    private fun fetchSessionsAndStudents() {
        val bookRef = db.collection("attendanceBooks").document(bookId)

        bookRef.collection("sessions").orderBy("startTime")
            .get()
            .addOnSuccessListener { sessionDocs ->
                sessionList.clear()
                for (doc in sessionDocs) {
                    val date = (doc.getTimestamp("startTime") ?: Timestamp.now())
                        .toDate().format("dd/MM/yyyy")
                    sessionList.add(Session(id = doc.id, date = date))
                }

                bookRef.get().addOnSuccessListener { bookDoc ->
                    val studentIds = bookDoc.get("students") as? List<String> ?: emptyList()
                    if (studentIds.isEmpty()) return@addOnSuccessListener

                    studentList.clear()
                    var loadedCount = 0

                    studentIds.forEach { uid ->
                        db.collection("students").document(uid).get()
                            .addOnSuccessListener { studentDoc ->
                                val student = Student(
                                    id = uid,
                                    usn = studentDoc.getString("usn") ?: "N/A",
                                    name = studentDoc.getString("fullName") ?: "N/A"
                                )

                                var sessionsLoaded = 0
                                sessionList.forEach { session ->
                                    val dateKey = session.date
                                    bookRef.collection("sessions")
                                        .document(session.id)
                                        .collection("records")
                                        .document(uid)
                                        .get()
                                        .addOnSuccessListener { recordDoc ->
                                            val status = recordDoc.getString("status") ?: "A"
                                            student.attendance[dateKey] = status
                                            sessionsLoaded++

                                            if (sessionsLoaded == sessionList.size) {
                                                studentList.add(student)
                                                loadedCount++
                                                if (loadedCount == studentIds.size) {
                                                    sendDataToWebView()
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            sessionsLoaded++
                                            student.attendance[dateKey] = "NA"
                                        }
                                }
                            }
                    }
                }
            }
    }

    /** ------------------- Send Data to WebView ------------------- **/
    private fun sendDataToWebView() {
        val gson = Gson()
        val jsonStudents = gson.toJson(studentList)
        val sessionData = sessionList.map { mapOf("id" to it.id, "date" to it.date) }
        val jsonSessions = gson.toJson(sessionData)
        webView.post {
            webView.evaluateJavascript(
                "loadAttendance($jsonStudents, $jsonSessions);",
                null
            )
        }

    }

    /** ------------------- Mark Attendance ------------------- **/
    private fun markAttendance(studentId: String, sessionId: String, status: String) {
        val docRef = db.collection("attendanceBooks")
            .document(bookId)
            .collection("sessions")
            .document(sessionId)
            .collection("records")
            .document(studentId)

        if (inProgressCheckIns.contains(studentId)) return
        inProgressCheckIns.add(studentId)

        val data = mapOf(
            "status" to status,
            "timestamp" to FieldValue.serverTimestamp()
        )

        docRef.set(data)
            .addOnSuccessListener {
                inProgressCheckIns.remove(studentId)
                // Only update local list
                val dateKey = sessionList.find { it.id == sessionId }?.date
                if (dateKey != null) {
                    studentList.find { it.id == studentId }?.attendance?.set(dateKey, status)
                }
                // DO NOT call sendDataToWebView() here
            }
            .addOnFailureListener { e ->
                Log.e("AttendanceSheet", "Failed to update attendance: ${e.message}")
                inProgressCheckIns.remove(studentId)
            }
    }


    private fun Date.format(pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}
