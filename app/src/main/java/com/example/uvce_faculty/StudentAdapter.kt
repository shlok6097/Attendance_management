package com.example.uvce_faculty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView

class StudentAdapter(private val students: List<Student>) :
    RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    private val selectedIds = mutableSetOf<String>()

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox = itemView.findViewById<CheckBox>(R.id.cbStudent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_checkbox, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        // Remove previous listener to prevent recycling issues
        holder.checkBox.setOnCheckedChangeListener(null)

        holder.checkBox.text = "${student.name} (${student.rollNumber})"
        holder.checkBox.isChecked = selectedIds.contains(student.id)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedIds.add(student.id)
            else selectedIds.remove(student.id)
        }
    }

    override fun getItemCount(): Int = students.size

    fun getSelectedStudentIds(): List<String> = selectedIds.toList()
}

