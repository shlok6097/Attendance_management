package com.example.uvce_faculty

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*
import kotlin.random.Random

class AttendanceSessionFragment : Fragment() {
    private val TAG = "ATTENDANCE_DEBUG"

    private lateinit var studentAdapter: StudentAttendanceAdapter
    private val studentList = mutableListOf<Student>()

    private lateinit var sessionId: String
    private var sessionCode: String = ""
    private var bookId = ""

    // UI
    private lateinit var tvTotalStudents: TextView
    private lateinit var tvTotalPresent: TextView
    private lateinit var tvTotalAbsent: TextView
    private lateinit var btnEndSession: Button
    private lateinit var btnToggleMode: Button
    private lateinit var tvGeneratedCode: TextView
    private var isManualMode = true

    // State guards - key = "studentId|sessionId" for better concurrency
    private val inProgressCheckIns = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_attendance_session, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bookId = arguments?.getString("bookId") ?: return

        tvTotalStudents = view.findViewById(R.id.tvTotalStudents)
        tvTotalPresent = view.findViewById(R.id.tvTotalPresent)
        tvTotalAbsent = view.findViewById(R.id.tvTotalAbsent)
        btnEndSession = view.findViewById(R.id.btnEndSession)
        btnToggleMode = view.findViewById(R.id.btnToggleMode)
        tvGeneratedCode = view.findViewById(R.id.tvGeneratedCode)

        val rv = view.findViewById<RecyclerView>(R.id.rvStudentList)
        studentAdapter = StudentAttendanceAdapter(studentList) { studentId, status ->
            markAttendance(studentId, status, "manual")
        }
        studentAdapter.isManualMode = isManualMode
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = studentAdapter

        createSession()

        btnEndSession.setOnClickListener { endSession() }
        btnToggleMode.setOnClickListener { toggleMode() }
    }

    /** ------------------- SESSION ------------------- **/
    private fun createSession() {
        val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("attendanceBooks")
            .document(bookId)
            .get()
            .addOnSuccessListener { bookDoc ->
                val studentIds = bookDoc.get("students") as? List<String> ?: emptyList()

                val session = hashMapOf(
                    "bookId" to bookId,
                    "teacherId" to teacherId,
                    "students" to studentIds,
                    "startTime" to FieldValue.serverTimestamp(),
                    "active" to true
                )

                FirebaseFirestore.getInstance()
                    .collection("attendanceBooks")
                    .document(bookId)
                    .collection("sessions")
                    .add(session)
                    .addOnSuccessListener { doc ->
                        sessionId = doc.id
                        Log.d(TAG, "✅ Session CREATED under book $bookId. SessionID: $sessionId")
                        fetchStudents()
                        observeAttendanceRecords()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to create session: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    private fun fetchStudents() {
        FirebaseFirestore.getInstance().collection("attendanceBooks")
            .document(bookId)
            .get()
            .addOnSuccessListener { doc ->
                val studentIds = doc.get("students") as? List<String> ?: emptyList()
                studentList.clear()
                for (uid in studentIds) {
                    FirebaseFirestore.getInstance().collection("students")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { studentDoc ->
                            if (!studentDoc.exists()) return@addOnSuccessListener
                            val student = Student(
                                id = uid,
                                usn = studentDoc.getString("usn") ?: "",
                                name = studentDoc.getString("fullName") ?: "",
                                status = "A" // default absent
                            )
                            studentList.add(student)
                            studentAdapter.notifyDataSetChanged()
                            updateAttendanceCounts()
                        }
                }
            }
    }

    /** ------------------- MARK ATTENDANCE ------------------- **/
    private fun markAttendance(studentId: String, status: String, mode: String) {
        val db = FirebaseFirestore.getInstance()

        // Composite record ID for uniqueness
        val recordId = "${bookId}_${sessionId}_$studentId"
        val docRef = db.collection("attendanceRecords").document(recordId)
        val opKey = "$studentId|$sessionId"

        if (inProgressCheckIns.contains(opKey)) {
            Log.d(TAG, "Skipping duplicate update for $opKey")
            return
        }
        inProgressCheckIns.add(opKey)

        val data = mapOf(
            "bookId" to bookId,
            "sessionId" to sessionId,
            "studentId" to studentId,
            "status" to status,
            "timestamp" to FieldValue.serverTimestamp(),
            "checkInType" to mode,
            "flagged" to false
        )

        Log.d(TAG, "Writing to Firestore: $opKey -> $status")
        docRef.set(data, SetOptions.merge()).addOnSuccessListener {
            inProgressCheckIns.remove(opKey)
            Log.d(TAG, "Firestore write SUCCESS for $opKey")
            studentList.find { it.id == studentId }?.status = status
            updateAttendanceCounts()
        }.addOnFailureListener { e ->
            inProgressCheckIns.remove(opKey)
            Log.e(TAG, "❌ Failed to mark attendance: ${e.message}")
        }
    }

    /** ------------------- OBSERVE ATTENDANCE ------------------- **/
    private fun observeAttendanceRecords() {
        FirebaseFirestore.getInstance()
            .collection("attendanceRecords")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("sessionId", sessionId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val statusMap = mutableMapOf<String, String>()
                snapshot?.documents?.forEach { doc ->
                    val studentId = doc.getString("studentId") ?: return@forEach
                    val status = doc.getString("status") ?: "A"
                    statusMap[studentId] = status
                }

                studentList.forEach { student ->
                    student.status = statusMap[student.id] ?: "A"
                }

                studentAdapter.notifyDataSetChanged()
                updateAttendanceCounts()
            }
    }

    /** ------------------- UPDATE COUNTS ------------------- **/
    private fun updateAttendanceCounts() {
        val totalStudents = studentList.size
        val present = studentList.count { it.status == "P" }
        val absent = totalStudents - present

        tvTotalStudents.text = "Total: $totalStudents"
        tvTotalPresent.text = "Present: $present"
        tvTotalAbsent.text = "Absent: $absent"
    }

    /** ------------------- MODE TOGGLE ------------------- **/
    private fun toggleMode() {
        isManualMode = !isManualMode
        btnToggleMode.text = if (isManualMode) "Switch to Auto Mode" else "Switch to Manual Mode"
        Toast.makeText(
            requireContext(),
            if (isManualMode) "Manual Mode Enabled" else "Auto Mode Enabled",
            Toast.LENGTH_SHORT
        ).show()

        studentAdapter.isManualMode = isManualMode

        val sessionRef = FirebaseFirestore.getInstance()
            .collection("attendanceBooks")
            .document(bookId)
            .collection("sessions")
            .document(sessionId)

        if (!isManualMode) {
            sessionCode = generateCode(4)
            tvGeneratedCode.text = "Code: $sessionCode"

            val expiresAt = Timestamp(Date(System.currentTimeMillis() + 5 * 60 * 1000))

            sessionRef.update(
                mapOf(
                    "code" to sessionCode,
                    "codeGeneratedAt" to FieldValue.serverTimestamp(),
                    "expiresAt" to expiresAt
                )
            )
        } else {
            tvGeneratedCode.text = ""
            sessionRef.update(
                mapOf(
                    "code" to FieldValue.delete(),
                    "expiresAt" to FieldValue.delete()
                )
            )
        }
    }

    /** ------------------- END SESSION ------------------- **/
    private fun endSession() {
        val db = FirebaseFirestore.getInstance()

        db.collection("attendanceRecords")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("sessionId", sessionId)
            .get()
            .addOnSuccessListener { snapshot ->
                val totalPresent = snapshot.documents.count { it.getString("status") == "P" }

                val sessionRef = db.collection("attendanceBooks")
                    .document(bookId)
                    .collection("sessions")
                    .document(sessionId)

                sessionRef.update(
                    mapOf(
                        "active" to false,
                        "endTime" to FieldValue.serverTimestamp(),
                        "totalPresent" to totalPresent
                    )
                ).addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Session ended. Present: $totalPresent",
                        Toast.LENGTH_LONG
                    ).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }.addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to update session: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to end session", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /** ------------------- Helper: Code Generator ------------------- **/
    private fun generateCode(length: Int): String {
        val chars = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789"
        return (1..length).map { chars.random(Random) }.joinToString("")
    }
}
