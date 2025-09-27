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
    private lateinit var rvHeader: RecyclerView
    private lateinit var rvStudents: RecyclerView
    private lateinit var adapter: AttendanceSheetAdapter
    private lateinit var dateAdapter: DateAdapter

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
        rvHeader = view.findViewById(R.id.rvDateHeader)
        rvStudents = view.findViewById(R.id.rvAttendanceSheet)

        // Setup student adapter
        adapter = AttendanceSheetAdapter(
            students = studentList,
            sessions = sessionList,
            onCellClick = { studentId, sessionId, status ->
                markAttendance(studentId, sessionId, status)
            }
        )
        rvStudents.layoutManager = LinearLayoutManager(requireContext())
        rvStudents.adapter = adapter

        fetchSessionsAndStudents()
    }

    private fun fetchSessionsAndStudents() {
        val bookRef = db.collection("attendanceBooks").document(bookId)

        // 1. Fetch sessions
        bookRef.collection("sessions").orderBy("startTime").get()
            .addOnSuccessListener { sessionDocs ->
                sessionList.clear()
                sessionDocs.forEach { doc ->
                    val date = (doc.getTimestamp("startTime") ?: Timestamp.now()).toDate()
                        .format("dd/MM/yyyy")
                    sessionList.add(Session(id = doc.id, date = date))
                }

                Log.d("AttendanceDebug", "Fetched ${sessionList.size} sessions: $sessionList")

                // Setup date header RecyclerView
                dateAdapter = DateAdapter(sessionList)
                rvHeader.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                rvHeader.adapter = dateAdapter

                // 2. Fetch students
                bookRef.get().addOnSuccessListener { bookDoc ->
                    val studentIds = bookDoc.get("students") as? List<String> ?: emptyList()
                    if (studentIds.isEmpty()) return@addOnSuccessListener

                    studentList.clear()
                    Log.d("AttendanceDebug", "Fetching ${studentIds.size} students")
                    fetchStudentAtIndex(0, studentIds, bookRef) // Pass DocumentReference
                }
            }
    }

    // Fetch one student and all their session records recursively
    private fun fetchStudentAtIndex(
        index: Int,
        studentIds: List<String>,
        bookRef: com.google.firebase.firestore.DocumentReference
    ) {
        if (index >= studentIds.size) {
            Log.d("AttendanceDebug", "All students loaded, updating adapter")
            adapter.updateData(studentList, sessionList)
            return
        }

        val uid = studentIds[index]
        db.collection("students").document(uid).get()
            .addOnSuccessListener { studentDoc ->
                val student = Student(
                    id = uid,
                    usn = studentDoc.getString("usn") ?: "N/A",
                    name = studentDoc.getString("fullName") ?: "N/A"
                )
                val attendanceMap = mutableMapOf<String, String>()
                var sessionsLoaded = 0

                sessionList.forEach { session ->
                    bookRef.collection("sessions").document(session.id)
                        .collection("records").document(uid).get()
                        .addOnSuccessListener { recordDoc ->
                            val status = recordDoc.getString("status") ?: "A"
                            attendanceMap[session.id] = status
                            sessionsLoaded++
                            Log.d(
                                "AttendanceDebug",
                                "Student ${student.name} session ${session.id} = $status"
                            )
                            if (sessionsLoaded == sessionList.size) {
                                student.attendance = attendanceMap
                                studentList.add(student)
                                fetchStudentAtIndex(index + 1, studentIds, bookRef)
                            }
                        }
                        .addOnFailureListener {
                            attendanceMap[session.id] = "A"
                            sessionsLoaded++
                            Log.d(
                                "AttendanceDebug",
                                "Failed to fetch session ${session.id} for ${student.name}, defaulting to A"
                            )
                            if (sessionsLoaded == sessionList.size) {
                                student.attendance = attendanceMap
                                studentList.add(student)
                                fetchStudentAtIndex(index + 1, studentIds, bookRef)
                            }
                        }
                }

                if (sessionList.isEmpty()) {
                    student.attendance = attendanceMap
                    studentList.add(student)
                    fetchStudentAtIndex(index + 1, studentIds, bookRef)
                }

            }
            .addOnFailureListener {
                Log.e("AttendanceDebug", "Failed to fetch student $uid, skipping")
                fetchStudentAtIndex(index + 1, studentIds, bookRef)
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
                val student = studentList.find { it.id == studentId }
                student?.attendance?.set(sessionId, status)
                adapter.updateStudent(studentId, sessionId, status)
                Log.d(
                    "AttendanceDebug",
                    "Marked attendance: student=$studentId, session=$sessionId, status=$status"
                )
            }
            .addOnFailureListener { e ->
                inProgressCheckIns.remove("$studentId-$sessionId")
                Log.e("AttendanceFragment", "Failed to mark attendance", e)
            }
    }

    private fun Date.format(pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}


