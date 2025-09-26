import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uvce_faculty.R
import com.example.uvce_faculty.Session
import com.example.uvce_faculty.Student

class AttendanceCellAdapter(
    private val student: Student,
    private val sessions: List<Session>,
    private val onCellClick: (studentId: String, sessionId: String, status: String) -> Unit
) : RecyclerView.Adapter<AttendanceCellAdapter.CellViewHolder>() {

    class CellViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_cell, parent, false)
        return CellViewHolder(view)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val session = sessions[position]
        val status = student.attendance[session.date] ?: "A"

        holder.tvStatus.text = status
        holder.tvStatus.setBackgroundColor(
            if (status == "P") {
                holder.itemView.context.getColor(android.R.color.holo_green_light)
            } else {
                holder.itemView.context.getColor(android.R.color.holo_red_light)
            }
        )

        holder.itemView.setOnClickListener {
            val newStatus = if (status == "P") "A" else "P"
            onCellClick(student.id, session.id, newStatus)
        }
    }

    override fun getItemCount(): Int = sessions.size
}
