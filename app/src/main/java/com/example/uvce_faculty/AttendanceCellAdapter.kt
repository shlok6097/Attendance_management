package com.example.uvce_faculty

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceCellAdapter(
    private val dates: List<String>,
    private val row: AttendanceRow,
    private val onCellClick: (date: String, newStatus: String) -> Unit
) : RecyclerView.Adapter<AttendanceCellAdapter.CellViewHolder>() {

    inner class CellViewHolder(val tvCell: TextView) : RecyclerView.ViewHolder(tvCell)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_cell, parent, false) as TextView
        return CellViewHolder(view)
    }

    override fun getItemCount(): Int = dates.size

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val date = dates[position]
        val status = row.attendance[date] ?: "A"
        holder.tvCell.text = status
        holder.tvCell.setBackgroundColor(if (status == "P") 0xFFB2FF59.toInt() else 0xFFFF8A80.toInt())

        holder.tvCell.setOnClickListener {
            val newStatus = if (status == "P") "A" else "P"
            row.attendance[date] = newStatus
            onCellClick(date, newStatus)
            notifyItemChanged(position)
        }
    }
}
