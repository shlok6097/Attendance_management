package com.example.uvce_faculty

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceBookAdapter(
    private val books: List<AttendanceBook>,
    private val onTakeAttendanceClick: (AttendanceBook) -> Unit,
    private val onViewBookClick: (AttendanceBook) -> Unit
) : RecyclerView.Adapter<AttendanceBookAdapter.BookViewHolder>() {

    // Define a list of background colors


    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBranch: TextView = itemView.findViewById(R.id.tvBranch)
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val btnTakeAttendance: Button = itemView.findViewById(R.id.btnTakeAttendance)
        val btnViewBook: Button = itemView.findViewById(R.id.btnViewBook)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.tvBranch.text = "Branch: ${book.branch} (Sem ${book.semester})"
        holder.tvSubject.text = "Subject: ${book.subject}"

        // Set a different background color for each item


        // Handle button clicks
        holder.btnTakeAttendance.setOnClickListener {
            onTakeAttendanceClick(book)
        }

        holder.btnViewBook.setOnClickListener {
            onViewBookClick(book)
        }
    }

    override fun getItemCount(): Int = books.size
}
