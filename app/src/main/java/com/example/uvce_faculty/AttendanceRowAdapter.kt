package com.example.uvce_faculty

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceRowAdapter(
    private val studentList: List<AttendanceRow>,
    private val sessionDates: List<String>,
    private val updateFirestore: (studentUid: String, date: String, status: String) -> Unit
) : RecyclerView.Adapter<AttendanceRowAdapter.RowViewHolder>() {

    inner class RowViewHolder(val container: LinearLayout) : RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_row, parent, false) as LinearLayout
        return RowViewHolder(view)
    }

    override fun getItemCount(): Int = studentList.size

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.container.removeAllViews()
        val student = studentList[position]

        // Sl No
        // Sl No
        holder.container.addView(createCell(holder.container.context, student.slNo.toString()))
        holder.container.addView(createCell(holder.container.context, student.usn))
        holder.container.addView(createCell(holder.container.context, student.name))

// Attendance cells
        sessionDates.forEach { date ->
            val tvCell = createCell(holder.container.context, student.attendance[date] ?: "A")
            tvCell.setOnClickListener {
                val newStatus = if (student.attendance[date] == "P") "A" else "P"
                student.attendance[date] = newStatus
                tvCell.text = newStatus
                tvCell.setBackgroundColor(if (newStatus == "P") 0xFFB2FF59.toInt() else 0xFFFF8A80.toInt())
                updateFirestore(student.uid, date, newStatus)
            }
            holder.container.addView(tvCell)
        }

// % column
        val total = sessionDates.size
        val attended = student.attendance.values.count { it == "P" }
        val percentCell = createCell(holder.container.context, "${(attended * 100) / total}%")
        holder.container.addView(percentCell)

    }

    private fun createCell(context: android.content.Context, text: String): TextView {
        val tv = TextView(context)
        tv.text = text
        tv.setBackgroundResource(R.drawable.cell_border)
        tv.gravity = Gravity.CENTER
        tv.width = 100
        tv.height = 80
        return tv
    }

}
