package com.example.uvce_faculty

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class AttendanceSheetFragment : Fragment() {

    private lateinit var bookId: String
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AttendanceSheetAdapter

    private val studentList = mutableListOf<Student>()
    private val sessionList = mutableListOf<Session>()
    private val inProgressCheckIns = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookId = arguments?.getString("bookId") ?: ""
        db = FirebaseFirestore.getInstance()
        Log.d("AttendanceFragment", "onCreate: bookId = $bookId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_attendance_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.rvAttendanceSheet)
        adapter = AttendanceSheetAdapter(
            students = studentList,
            sessions = sessionList,
            onCellClick = { studentId, sessionId, status ->
                markAttendance(studentId, sessionId, status)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchSessionsAndStudents()
    }

    private fun fetchSessionsAndStudents() {
        Log.d("AttendanceFragment", "Fetching sessions and students from DB...")
        val bookRef = db.collection("attendanceBooks").document(bookId)

        bookRef.collection("sessions").orderBy("startTime")
            .get().addOnSuccessListener { sessionDocs ->
                sessionList.clear()
                for (doc in sessionDocs) {
                    val date = (doc.getTimestamp("startTime") ?: Timestamp.now()).toDate()
                        .format("dd/MM/yyyy")
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
                                    val sessionId = session.id
                                    bookRef.collection("sessions")
                                        .document(sessionId)
                                        .collection("records")
                                        .document(uid)
                                        .get()
                                        .addOnSuccessListener { recordDoc ->
                                            val status = recordDoc.getString("status") ?: "A"
                                            student.attendance[sessionId] = status
                                            sessionsLoaded++
                                            if (sessionsLoaded == sessionList.size) {
                                                studentList.add(student)
                                                loadedCount++
                                                if (loadedCount == studentIds.size) {
                                                    adapter.updateData(studentList, sessionList)
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            sessionsLoaded++
                                            student.attendance[sessionId] = "A"
                                        }
                                }
                            }
                    }
                }
            }
    }

    private fun markAttendance(studentId: String, sessionId: String, status: String) {
        if (inProgressCheckIns.contains("$studentId-$sessionId")) return
        inProgressCheckIns.add("$studentId-$sessionId")

        val docRef = db.collection("attendanceBooks")
            .document(bookId)
            .collection("sessions")
            .document(sessionId)
            .collection("records")
            .document(studentId)

        val data = mapOf("status" to status)

        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                inProgressCheckIns.remove("$studentId-$sessionId")
                // Update local model and notify adapter
                val student = studentList.find { it.id == studentId }
                student?.attendance?.set(sessionId, status)
                adapter.updateStudent(studentId, sessionId, status)
            }
            .addOnFailureListener { e ->
                inProgressCheckIns.remove("$studentId-$sessionId")
                Log.e("AttendanceFragment", "Failed to mark attendance", e)
            }
    }

    private fun Date.format(pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}
