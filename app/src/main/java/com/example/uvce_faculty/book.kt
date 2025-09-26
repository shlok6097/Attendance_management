package com.example.uvce_faculty

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class book : Fragment() {

    private lateinit var studentAdapter: StudentAdapter
    private var studentList = mutableListOf<Student>()

    private val branchList = listOf("CSE", "ECE", "MECH")
    private val semesterList = (1..8).map { it.toString() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book, container, false)

        val spinnerBranch = view.findViewById<Spinner>(R.id.spinnerBranch)
        val spinnerSemester = view.findViewById<Spinner>(R.id.spinnerSemester)
        val etSubject = view.findViewById<EditText>(R.id.etSubject)
        val rvStudents = view.findViewById<RecyclerView>(R.id.rvStudents)
        val btnCreateBook = view.findViewById<Button>(R.id.btnCreateBook)

        // Set spinners
        spinnerBranch.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, branchList)
        spinnerSemester.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, semesterList)

        // RecyclerView setup
        studentAdapter = StudentAdapter(studentList)
        rvStudents.layoutManager = LinearLayoutManager(requireContext())
        rvStudents.adapter = studentAdapter

        // Fetch students function
        fun fetchStudents() {
            val branch = spinnerBranch.selectedItem.toString()
            val semester = spinnerSemester.selectedItem.toString() // as string

            FirebaseFirestore.getInstance()
                .collection("students")
                .whereEqualTo("branch", branch)
                .whereEqualTo("semester", semester)
                .get()
                .addOnSuccessListener { snapshot ->
                    studentList.clear()
                    for (doc in snapshot.documents) {
                        val student = Student(
                            id = doc.getString("uid") ?: "",             // use uid
                            name = doc.getString("fullName") ?: "",      // full name
                            rollNumber = doc.getString("usn") ?: ""     // usn as roll number
                        )
                        studentList.add(student)
                    }
                    studentAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching students: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }


        // Call fetchStudents when any spinner changes
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fetchStudents()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerBranch.onItemSelectedListener = listener
        spinnerSemester.onItemSelectedListener = listener

        // Create Attendance Book
        btnCreateBook.setOnClickListener {
            val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val subject = etSubject.text.toString().trim()
            val selectedStudentIds = studentAdapter.getSelectedStudentIds()

            if (subject.isEmpty()) {
                Toast.makeText(requireContext(), "Enter subject", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedStudentIds.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one student", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val book = hashMapOf(
                "teacherId" to teacherId,
                "branch" to spinnerBranch.selectedItem.toString(),
                "semester" to spinnerSemester.selectedItem.toString().toInt(),
                "subject" to subject,
                "students" to selectedStudentIds,
                "createdAt" to FieldValue.serverTimestamp()
            )

            FirebaseFirestore.getInstance()
                .collection("attendanceBooks")
                .add(book)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Attendance Book Created!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}


