package com.example.uvce_faculty

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.example.uvce_faculty.databinding.FragmentLogInBinding

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LogIn : Fragment() {

    private var _binding: FragmentLogInBinding? = null
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
        _binding = FragmentLogInBinding.inflate(inflater, container, false)

        binding.loginButton.setOnClickListener {
            loginAdmin()
        }

        return binding.root
    }

    private fun loginAdmin() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress indicator
        showProgress(true)

        // Authenticate using Firebase Auth
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""

                // Verify role in Firestore
                db.collection("admins").document(uid).get()
                    .addOnSuccessListener { document ->
                        showProgress(false)
                        if (document.exists()) {
                            val role = document.getString("role")
                            if (role == "Admin") {
                                Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                                val intent = Intent(requireActivity(), MainActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                            // Navigate to home or dashboard
                                // findNavController().navigate(R.id.action_login_to_home)
                            } else {
                                auth.signOut()
                                Toast.makeText(requireContext(), "You are not authorized", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            auth.signOut()
                            Toast.makeText(requireContext(), "Admin account not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        showProgress(false)
                        auth.signOut()
                        Toast.makeText(requireContext(), "Firestore Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }

            }
            .addOnFailureListener { e ->
                showProgress(false)
                Toast.makeText(requireContext(), "Auth Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.loginButton.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.loginButton.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
