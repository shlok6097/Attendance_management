package com.example.uvce_faculty

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uvce_faculty.databinding.FragmentNotificationsBinding // Assuming you are using ViewBinding
import java.util.Date

// Placeholder data class, replace with your actual data model if different
// data class UpcomingTaskItem(
//     val taskId: String,
//     val taskTitle: String,
//     val subject: String,
//     val dueDateTime: com.google.firebase.Timestamp? // Or your Date type
// )

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var upcomingTasksAdapter: UpcomingTasksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadNotifications() // Call this to load your data
    }

    private fun setupRecyclerView() {
        upcomingTasksAdapter = UpcomingTasksAdapter { taskItem ->
            // Handle task item click if needed
            Log.d("NotificationsFragment", "Clicked on task: ${'$'}{taskItem.taskTitle}")
        }
        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = upcomingTasksAdapter
        }
        Log.d("NotificationsFragment", "RecyclerView setup complete. Adapter attached.")
    }

    private fun loadNotifications() {
        // TODO: Replace this with your actual data fetching logic
        // For example, fetch from a ViewModel, Firebase, or local database
        val dummyTasks = listOf(
            UpcomingTaskItem("1", "Submit Assignment 1", "Data Structures", com.google.firebase.Timestamp(Date(System.currentTimeMillis() + 86400000L * 2))), // Due in 2 days
            UpcomingTaskItem("2", "Prepare for Quiz", "Algorithms", com.google.firebase.Timestamp(Date(System.currentTimeMillis() + 86400000L * 5))), // Due in 5 days
            UpcomingTaskItem("3", "Project Phase 1", "Software Engineering", null) // No due date
        )
        upcomingTasksAdapter.updateTasks(dummyTasks)
        Log.d("NotificationsFragment", "Dummy tasks loaded into adapter.")

        if (dummyTasks.isEmpty()) {
            Log.w("NotificationsFragment", "No tasks loaded, RecyclerView might appear empty.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("NotificationsFragment", "onDestroyView called, cleaning up binding.")
        _binding = null // Important to avoid memory leaks
    }
}
