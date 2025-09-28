package com.example.uvce_faculty

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.uvce_faculty.databinding.ItemUpcomingTaskBinding
import java.text.SimpleDateFormat
import java.util.Locale

class UpcomingTasksAdapter(
    private val onItemClicked: (UpcomingTaskItem) -> Unit
) : RecyclerView.Adapter<UpcomingTasksAdapter.TaskViewHolder>() {

    private val tasks = mutableListOf<UpcomingTaskItem>()
    private val dueDateFormat = SimpleDateFormat("EEE, MMM d, hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemUpcomingTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<UpcomingTaskItem>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
        Log.d("UpcomingTasksAdapter", "Adapter updated with ${tasks.size} tasks")
    }

    inner class TaskViewHolder(private val binding: ItemUpcomingTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClicked(tasks[position])
                }
            }
        }

        fun bind(task: UpcomingTaskItem) {
            binding.tvItemTaskTitle.text = task.taskTitle
            binding.tvItemTaskSubject.text = if (task.subject.isNotEmpty()) "Subject: ${task.subject}" else ""
            binding.tvItemTaskDueDate.text = task.dueDateTime?.toDate()?.let {
                "Due: ${dueDateFormat.format(it)}"
            } ?: "No due date"
        }
    }
}

