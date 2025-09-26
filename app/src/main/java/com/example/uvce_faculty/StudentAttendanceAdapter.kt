package com.example.uvce_faculty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentAttendanceAdapter(
    private val students: List<Student>,
    private val onMarkAttendance: (studentId: String, status: String) -> Unit
) : RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder>() {

    var isManualMode: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged() // Refresh UI when mode changes
        }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSlNo: TextView = itemView.findViewById(R.id.tvSlNo)
        val tvRollNo: TextView = itemView.findViewById(R.id.tvRollNo)
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val rgAttendance: RadioGroup = itemView.findViewById(R.id.rgAttendance)
        val rbPresent: RadioButton = itemView.findViewById(R.id.rbPresent)
        val rbAbsent: RadioButton = itemView.findViewById(R.id.rbAbsent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]

        holder.tvSlNo.text = (position + 1).toString()
        holder.tvRollNo.text = student.usn
        holder.tvStudentName.text = student.name

        holder.rbPresent.isEnabled = isManualMode
        holder.rbAbsent.isEnabled = isManualMode

        // Clear previous listener before changing checked state
        holder.rgAttendance.setOnCheckedChangeListener(null)

        // Set RadioButton based on current status
        when (student.status) {
            "P" -> holder.rbPresent.isChecked = true
            "A" -> holder.rbAbsent.isChecked = true
            else -> holder.rgAttendance.clearCheck()
        }

        // Set listener only in manual mode
        if (isManualMode) {
            holder.rgAttendance.setOnCheckedChangeListener { _, checkedId ->
                val newStatus = when (checkedId) {
                    R.id.rbPresent -> "P"
                    R.id.rbAbsent -> "A"
                    else -> return@setOnCheckedChangeListener
                }

                // Only trigger if status changed
                if (student.status != newStatus) {
                    student.status = newStatus
                    onMarkAttendance(student.id, newStatus)
                }
            }
        }
    }

    override fun getItemCount() = students.size
}
