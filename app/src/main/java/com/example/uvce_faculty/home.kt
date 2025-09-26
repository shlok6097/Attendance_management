package com.example.uvce_faculty

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.uvce_faculty.R


class home : Fragment() {

    private lateinit var bookAdapter: AttendanceBookAdapter
    private var bookList = mutableListOf<AttendanceBook>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val rvBooks = view.findViewById<RecyclerView>(R.id.rvBooks)

        // ✅ Initialize adapter with two click listeners
        bookAdapter = AttendanceBookAdapter(
            bookList,
            onTakeAttendanceClick = { book ->
                val bundle = Bundle().apply {
                    putString("bookId", book.id)
                    putString("branch", book.branch)
                    putString("semester", book.semester)
                    putString("subject", book.subject)
                }
                findNavController().navigate(R.id.attendanceSessionFragment, bundle)
            },
            onViewBookClick = { book ->
                val bundle = Bundle().apply {
                    putString("bookId", book.id)
                    putString("branch", book.branch)
                    putString("semester", book.semester)
                    putString("subject", book.subject)
                }
                android.util.Log.d("HomeFragment", "Navigating to AttendanceSheetFragment with bookId=${book.id}")
                findNavController().navigate(R.id.attendanceSheetFragment, bundle)
            }
        )

        rvBooks.layoutManager = LinearLayoutManager(requireContext())
        rvBooks.adapter = bookAdapter

        loadAttendanceBooks()

        return view
    }

    private fun loadAttendanceBooks() {
        val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("attendanceBooks")
            .whereEqualTo("teacherId", teacherId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                bookList.clear()
                for (doc in snapshot.documents) {
                    val book = AttendanceBook(
                        id = doc.id,
                        branch = doc.getString("branch") ?: "",
                        semester = doc.get("semester")?.toString() ?: "",
                        subject = doc.getString("subject") ?: "",
                        students = doc.get("students") as? List<String> ?: emptyList()
                    )
                    bookList.add(book)
                }
                bookAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

