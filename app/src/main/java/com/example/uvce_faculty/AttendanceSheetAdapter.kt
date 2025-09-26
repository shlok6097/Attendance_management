package com.example.uvce_faculty

import AttendanceCellAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt // Import for rounding

// Payload class for partial updates
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

    // Full bind logic
    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.tvSlNo.text = (position + 1).toString() // Consider R.string for "Sl No. %d"
        holder.tvUSN.text = student.usn
        holder.tvName.text = student.name

        // Calculate percentage
        val attendedCount = sessions.count { session ->
            student.attendance[session.date] == "P"
        }
        val percentage = if (sessions.isNotEmpty()) {
            (attendedCount.toFloat() / sessions.size * 100).roundToInt()
        } else 0
        // Consider using string resources: holder.itemView.context.getString(R.string.percentage_format, percentage)
        holder.tvPercentage.text = holder.itemView.context.getString(R.string.percentage_display, percentage)


        // Setup attendance cells RecyclerView
        val cellAdapter = AttendanceCellAdapter(
            student = student,
            sessions = sessions,
            onCellClick = this.onCellClick // Propagate the click handler
        )
        holder.rvAttendanceCells.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            holder.itemView.context,
            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
            false
        )
        holder.rvAttendanceCells.adapter = cellAdapter
    }

    // Partial bind logic (with payloads)
    override fun onBindViewHolder(holder: StudentViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            // No payload, do a full bind by calling the other onBindViewHolder
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val student = students[position] // Get the current student data

            payloads.forEach { payload ->
                if (payload is AttendanceCellUpdatePayload) {
                    // 1. Update the percentage text (it's affected by cell change)
                    val attendedCount = sessions.count { s -> student.attendance[s.date] == "P" }
                    val percentage = if (sessions.isNotEmpty()) {
                        (attendedCount.toFloat() / sessions.size * 100).roundToInt()
                    } else 0
                    holder.tvPercentage.text = holder.itemView.context.getString(R.string.percentage_display, percentage)


                    // 2. Notify the inner AttendanceCellAdapter to update the specific cell
                    val cellAdapter = holder.rvAttendanceCells.adapter as? AttendanceCellAdapter
                    // Use this.sessions (or just sessions) from AttendanceSheetAdapter's scope
                    val sessionIndexToUpdate = this.sessions.indexOfFirst { it.id == payload.sessionId }

                    if (cellAdapter != null && sessionIndexToUpdate != -1) { // Check sessionIndexToUpdate directly
                        cellAdapter.notifyItemChanged(sessionIndexToUpdate)
                    } else {
                        // Fallback if specific update i// Less ideal, but a safe fallbacksn't possible (e.g., session not found)
                        cellAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }


    override fun getItemCount(): Int = students.size

    fun updateData(newStudents: List<Student>, newSessions: List<Session>) {
        students = newStudents
        sessions = newSessions
        notifyDataSetChanged()
    }

    // This function now triggers a partial update via payload
    fun updateStudent(studentId: String, sessionId: String, status: String) {
        val studentIndex = students.indexOfFirst { it.id == studentId }
        if (studentIndex != -1) {
            val student = students[studentIndex]
            val session = sessions.find { it.id == sessionId }
            if (session != null) {
                student.attendance[session.date] = status // Update data model

                // Prepare payload for partial update
                val payload = AttendanceCellUpdatePayload(sessionId = session.id)
                notifyItemChanged(studentIndex, payload)
            }
        }
    }
}
