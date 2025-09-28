package com.example.uvce_faculty

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uvce_faculty.databinding.FragmentNotificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var upcomingTasksAdapter: UpcomingTasksAdapter

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val TAG = "NotificationFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        fetchUpcomingTasks()
    }

    private fun setupRecyclerView() {
        // Adapter starts empty
        upcomingTasksAdapter = UpcomingTasksAdapter { task ->
            Toast.makeText(requireContext(), "Clicked on: ${task.taskTitle}", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Task clicked: ${task.taskTitle}, id=${task.id}")
        }
        binding.rvUpcomingTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingTasksAdapter
        }
    }

    private fun fetchUpcomingTasks() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not logged in. Cannot fetch tasks.")
            binding.tvNoUpcomingTasks.text = "Please login to see tasks."
            binding.tvNoUpcomingTasks.isVisible = true
            binding.rvUpcomingTasks.isVisible = false
            return
        }

        Log.d(TAG, "Fetching upcoming tasks for user: ${currentUser.uid}")

        binding.tvNoUpcomingTasks.isVisible = false
        binding.rvUpcomingTasks.isVisible = true

        firestore.collection("admins").document(currentUser.uid)
            .collection("tasks")
            .whereEqualTo("isCompleted", false)
            .whereGreaterThan("dueDateTime", Timestamp.now())
            .orderBy("dueDateTime", Query.Direction.ASCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Query successful. Found ${documents.size()} documents.")

                if (documents.isEmpty) {
                    Log.d(TAG, "No upcoming tasks found.")
                    binding.tvNoUpcomingTasks.text = getString(R.string.no_upcoming_tasks)
                    binding.tvNoUpcomingTasks.isVisible = true
                    binding.rvUpcomingTasks.isVisible = false
                    upcomingTasksAdapter.updateTasks(emptyList())
                } else {
                    val fetchedTasks = mutableListOf<UpcomingTaskItem>()
                    for (document in documents) {
                        Log.d(TAG, "Document fetched: ${document.id} => ${document.data}")
                        val task = document.toObject(UpcomingTaskItem::class.java).copy(id = document.id)
                        fetchedTasks.add(task)
                    }
                    Log.d(TAG, "Final task list size: ${fetchedTasks.size}")
                    upcomingTasksAdapter.updateTasks(fetchedTasks)
                    binding.tvNoUpcomingTasks.isVisible = false
                    binding.rvUpcomingTasks.isVisible = true
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching tasks", exception)
                binding.tvNoUpcomingTasks.text = "Error fetching tasks: ${exception.message}"
                binding.tvNoUpcomingTasks.isVisible = true
                binding.rvUpcomingTasks.isVisible = false
                Toast.makeText(requireContext(), "Error fetching tasks: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
