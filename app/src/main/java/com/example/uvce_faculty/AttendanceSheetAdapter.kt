package com.example.uvce_faculty

import AttendanceCellAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

data class AttendanceCellUpdatePayload(val sessionId: String)

class AttendanceSheetAdapter(
    private var students: List<Student>,
    private var sessions: List<Session>,
    private val onCellClick: (studentId: String, sessionId: String, status: String) -> Unit
) : RecyclerView.Adapter<AttendanceSheetAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSlNo: TextView = itemView.findViewById(R.id.tvSlNo)
        val tvUSN: TextView = itemView.findViewById(R.id.tvUSN)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val rvAttendanceCells: RecyclerView = itemView.findViewById(R.id.rvAttendanceCells)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_sheet_row, parent, false)
        return StudentViewHolder(view)
    }

    override fun getItemCount(): Int = students.size

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.tvSlNo.text = (position + 1).toString()
        holder.tvUSN.text = student.usn
        holder.tvName.text = student.name

        updatePercentage(holder, student)

        // Inner RecyclerView for attendance cells
        val cellAdapter = AttendanceCellAdapter(
            student = student,
            sessions = sessions,
            onCellClick = { studentId, sessionId, newStatus ->
                student.attendance[sessionId] = newStatus
                onCellClick(studentId, sessionId, newStatus)

                // Update percentage
                updatePercentage(holder, student)

                // Update only that cell
                val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
                if (sessionIndex != -1) {
                    holder.rvAttendanceCells.adapter?.notifyItemChanged(sessionIndex)
                }
            }
        )

        holder.rvAttendanceCells.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.rvAttendanceCells.adapter = cellAdapter
    }

    override fun onBindViewHolder(
        holder: StudentViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val student = students[position]
            payloads.forEach { payload ->
                if (payload is AttendanceCellUpdatePayload) {
                    updatePercentage(holder, student)
                    val cellAdapter = holder.rvAttendanceCells.adapter as? AttendanceCellAdapter
                    val sessionIndex = sessions.indexOfFirst { it.id == payload.sessionId }
                    if (cellAdapter != null && sessionIndex != -1) {
                        cellAdapter.notifyItemChanged(sessionIndex)
                    } else {
                        cellAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun updatePercentage(holder: StudentViewHolder, student: Student) {
        val attendedCount = sessions.count { session ->
            student.attendance[session.id]?.let { it == "P" } ?: false
        }
        val percentage = if (sessions.isNotEmpty()) (attendedCount.toFloat() / sessions.size * 100).roundToInt() else 0
        holder.tvPercentage.text = holder.itemView.context.getString(R.string.percentage_display, percentage)
    }

    fun updateData(newStudents: List<Student>, newSessions: List<Session>) {
        students = newStudents
        sessions = newSessions
        notifyDataSetChanged()
    }

    fun updateStudent(studentId: String, sessionId: String, status: String) {
        val studentIndex = students.indexOfFirst { it.id == studentId }
        if (studentIndex != -1) {
            val student = students[studentIndex]
            student.attendance[sessionId] = status
            notifyItemChanged(studentIndex, AttendanceCellUpdatePayload(sessionId))
        }
    }
}
