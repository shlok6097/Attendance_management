package com.example.uvce_faculty

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uvce_faculty.databinding.FragmentBookBinding // Import ViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Assuming Student class is defined elsewhere

class book : Fragment() { // Ensured class name is BookFragment

    private var _binding: FragmentBookBinding? = null
    private val binding get() = _binding!!

    private lateinit var studentAdapter: StudentAdapter
    private var studentList = mutableListOf<Student>()

    private val branchList = listOf("CSE", "ISE", "AIML/AIDS", "ECE", "MECH", "CIVIL", "EEE")
    private val semesterList = (1..8).map { it.toString() }

    private val selectedTaskDueDate = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMainFormSpinners()
        setupRecyclerView()
        setupStudentFetchingListeners()
        setupCreateBookButton()

        // --- New Task Form Logic ---
        setupTaskFormSpinners()
        setupTaskDateTimePickers()
        setupManageTasksButton()
        setupSaveTaskButton()
    }

    private fun setupMainFormSpinners() {
        binding.spinnerBranch.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, branchList)
        binding.spinnerSemester.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, semesterList)
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(studentList) // Assuming StudentAdapter is correctly implemented
        binding.rvStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudents.adapter = studentAdapter
    }

    private fun fetchStudents() {
        val branch = binding.spinnerBranch.selectedItem.toString()
        val semester = binding.spinnerSemester.selectedItem.toString()

        FirebaseFirestore.getInstance()
            .collection("students")
            .whereEqualTo("branch", branch)
            .whereEqualTo("semester", semester)
            .get()
            .addOnSuccessListener { snapshot ->
                studentList.clear()
                for (doc in snapshot.documents) {
                    val student = Student(
                        id = doc.getString("uid") ?: "",
                        name = doc.getString("fullName") ?: "",
                        rollNumber = doc.getString("usn") ?: ""
                    )
                    studentList.add(student)
                }
                studentAdapter.notifyDataSetChanged() // Consider more specific notify events if possible
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching students: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupStudentFetchingListeners() {
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fetchStudents()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spinnerBranch.onItemSelectedListener = listener
        binding.spinnerSemester.onItemSelectedListener = listener
    }

    private fun setupCreateBookButton() {
        binding.btnCreateBook.setOnClickListener {
            val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val subject = binding.etSubject.text.toString().trim()
            val selectedStudentIds = studentAdapter.getSelectedStudentIds()

            if (subject.isEmpty()) {
                Toast.makeText(requireContext(), "Enter subject", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedStudentIds.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one student", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val book = hashMapOf(
                "teacherId" to teacherId,
                "branch" to binding.spinnerBranch.selectedItem.toString(),
                "semester" to binding.spinnerSemester.selectedItem.toString().toInt(),
                "subject" to subject,
                "students" to selectedStudentIds,
                "createdAt" to FieldValue.serverTimestamp()
            )

            FirebaseFirestore.getInstance().collection("attendanceBooks").add(book)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Attendance Book Created!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupManageTasksButton() {
        binding.btnManageTasks.setOnClickListener {
            binding.addTaskFormLayout.isVisible = !binding.addTaskFormLayout.isVisible
        }
    }

    private fun setupTaskFormSpinners() {
        ArrayAdapter.createFromResource(requireContext(), R.array.reminder_options, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerTaskReminder.adapter = adapter
        }
        ArrayAdapter.createFromResource(requireContext(), R.array.priority_options, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerTaskPriority.adapter = adapter
        }
    }

    private fun setupTaskDateTimePickers() {
        binding.btnTaskDueDate.setOnClickListener {
            val currentDate = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                selectedTaskDueDate.set(Calendar.YEAR, year)
                selectedTaskDueDate.set(Calendar.MONTH, month)
                selectedTaskDueDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateSelectedDateTimeTextView()
            }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.btnTaskDueTime.setOnClickListener {
            val currentTime = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                selectedTaskDueDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTaskDueDate.set(Calendar.MINUTE, minute)
                selectedTaskDueDate.set(Calendar.SECOND, 0)
                selectedTaskDueDate.set(Calendar.MILLISECOND, 0)
                updateSelectedDateTimeTextView()
            }, currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), true
            ).show()
        }
    }

    private fun updateSelectedDateTimeTextView() {
        val displayText = "Selected: ${dateFormatter.format(selectedTaskDueDate.time)}"
        binding.taskSelectedDateTimeTextView.text = displayText // Use a string resource for "Selected: %s"
        binding.taskSelectedDateTimeTextView.isVisible = true
    }

    private fun setupSaveTaskButton() {
        binding.btnSaveTask.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Please login to save tasks", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val taskTitle = binding.taskTitleEditText.text.toString().trim()
            if (taskTitle.isEmpty()) {
                binding.taskTitleInputLayout.error = "Task title cannot be empty" // Use string resource
                return@setOnClickListener
            }
            binding.taskTitleInputLayout.error = null

            val taskDescription = binding.taskDescriptionEditText.text.toString().trim()
            val reminderOption = binding.spinnerTaskReminder.selectedItem.toString()
            val priorityOption = binding.spinnerTaskPriority.selectedItem.toString()

            if (!binding.taskSelectedDateTimeTextView.isVisible || binding.taskSelectedDateTimeTextView.text.toString().startsWith("Selected:").not()) {
                Toast.makeText(requireContext(), "Please select a due date and time", Toast.LENGTH_SHORT).show() // Use string resource
                return@setOnClickListener
            }
            val dueDateTimeMillis = selectedTaskDueDate.timeInMillis

            val taskData = hashMapOf(
                "branch" to binding.spinnerBranch.selectedItem.toString(),
                "semester" to binding.spinnerSemester.selectedItem.toString(),
                "subject" to binding.etSubject.text.toString().trim(),
                "taskTitle" to taskTitle,
                "taskDescription" to taskDescription,
                "dueDateTime" to com.google.firebase.Timestamp(selectedTaskDueDate.time),
                "reminderOption" to reminderOption,
                "priority" to priorityOption,
                "isCompleted" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )

            FirebaseFirestore.getInstance()
                .collection("admins") // Corrected to "users"
                .document(currentUser.uid)
                .collection("tasks")
                .add(taskData)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(requireContext(), "Task saved!", Toast.LENGTH_SHORT).show() // Use string resource
                    val taskId = documentReference.id // Get the auto-generated task ID

                    // Schedule notification
                    val triggerTime = NotificationScheduler.calculateTriggerTime(dueDateTimeMillis, reminderOption)
                    if (triggerTime > System.currentTimeMillis()) { // Only schedule if time is in the future
                        NotificationScheduler.scheduleTaskReminder(
                            requireContext(),
                            taskId,
                            taskTitle,
                            taskDescription,
                            triggerTime
                        )
                         Toast.makeText(requireContext(), "Reminder scheduled for $taskTitle", Toast.LENGTH_SHORT).show()
                    } else if (reminderOption != "No reminder") {
                         Toast.makeText(requireContext(), "Reminder time is in the past.", Toast.LENGTH_SHORT).show()
                    }

                    clearAndHideTaskForm()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error saving task: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun clearAndHideTaskForm() {
        binding.taskTitleEditText.text = null
        binding.taskDescriptionEditText.text = null
        binding.taskSelectedDateTimeTextView.text = "" // Use string resource or make it an empty string
        binding.taskSelectedDateTimeTextView.isVisible = false
        binding.spinnerTaskReminder.setSelection(0)
        binding.spinnerTaskPriority.setSelection(0)
        binding.addTaskFormLayout.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
