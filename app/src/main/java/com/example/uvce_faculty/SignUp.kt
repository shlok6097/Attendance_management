package com.example.uvce_faculty

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.uvce_faculty.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUp : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)

        binding.signup.setOnClickListener {
            registerAdmin()
        }

        return binding.root
    }

    private fun registerAdmin() {
        val fullName = binding.name.editText?.text.toString().trim()
        val mobile = binding.mobile.editText?.text.toString().trim()
        val staffId = binding.staffid.editText?.text.toString().trim()
        val email = binding.email.editText?.text.toString().trim()
        val password = binding.password.editText?.text.toString().trim()
        val confirmPassword = binding.confirmPassword.editText?.text.toString().trim()

        // Input validation
        if (fullName.isEmpty() || mobile.isEmpty() || staffId.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Show Material Progress Bar
        binding.progressBar.visibility = View.VISIBLE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                val adminData = hashMapOf(
                    "uid" to uid,
                    "fullName" to fullName,
                    "mobile" to mobile,
                    "staffId" to staffId,
                    "email" to email,
                    "instituteId" to uid, // or generate custom ID
                    "createdAt" to System.currentTimeMillis(),
                    "role" to "Admin"
                )

                db.collection("admins").document(uid).set(adminData)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Account Created Successfully!", Toast.LENGTH_LONG).show()
                        clearFields()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Firestore Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Auth Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearFields() {
        binding.name.editText?.setText("")
        binding.mobile.editText?.setText("")
        binding.staffid.editText?.setText("")
        binding.email.editText?.setText("")
        binding.password.editText?.setText("")
        binding.confirmPassword.editText?.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
