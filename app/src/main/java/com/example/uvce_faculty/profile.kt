package com.example.uvce_faculty

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.uvce_faculty.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class profile : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var isEditMode = false
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var profileImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFieldControls(isEditMode)
        loadProfileData()

        // Toggle edit/save
        binding.editSaveButton.setOnClickListener {
            isEditMode = !isEditMode
            setupFieldControls(isEditMode)
            binding.editSaveButton.text = if (isEditMode) "Save Profile" else "Edit Profile"

            if (!isEditMode) saveProfileData()
        }

        // Change profile image
        binding.editProfileImageButton.setOnClickListener {
            selectProfileImage()
        }

        // Expandable sections
        binding.logoutHeader.setOnClickListener {
            binding.logoutContent.isVisible = !binding.logoutContent.isVisible
        }

        binding.feedbackHeader.setOnClickListener {
            binding.feedbackContent.isVisible = !binding.feedbackContent.isVisible
        }

        binding.helpHeader.setOnClickListener {
            binding.helpContent.isVisible = !binding.helpContent.isVisible
        }

        // Logout
        binding.confirmLogoutButton.setOnClickListener {
            auth.signOut()
            showLogoutDialog()

        }

        // Feedback
        binding.submitFeedbackButton.setOnClickListener {
            val feedbackEditText = binding.feedbackContent.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.feedbackEditText)
            val feedback = feedbackEditText.text.toString().trim()
            if (feedback.isNotEmpty()) {
                val feedbackData = hashMapOf(
                    "uid" to auth.currentUser?.uid,
                    "feedback" to feedback,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("feedback").add(feedbackData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Feedback submitted", Toast.LENGTH_SHORT).show()
                        binding.feedbackContent.isVisible = false
                        feedbackEditText.text?.clear()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "Please enter feedback", Toast.LENGTH_SHORT).show()
            }
        }

        // FAQ
        binding.faqButton.setOnClickListener {
            // Open FAQ webpage
            Toast.makeText(context, "Visit FAQ clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFieldControls(enable: Boolean) {
        binding.nameEditText.isEnabled = enable
        binding.userIdEditText.isEnabled = enable
        binding.emailEditText.isEnabled = enable
        binding.departmentEditText.isEnabled = enable
        binding.designationEditText.isEnabled = enable
        binding.officeLocationEditText.isEnabled = enable
        binding.contactNumberEditText.isEnabled = enable
    }

    private fun loadProfileData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("admins").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.nameEditText.setText(doc.getString("fullName"))
                    binding.userIdEditText.setText(doc.getString("staffId"))
                    binding.emailEditText.setText(doc.getString("email"))
                    binding.departmentEditText.setText(doc.getString("department") ?: "")
                    binding.designationEditText.setText(doc.getString("designation") ?: "")
                    binding.officeLocationEditText.setText(doc.getString("officeLocation") ?: "")
                    binding.contactNumberEditText.setText(doc.getString("mobile") ?: "")

                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileData() {
        val uid = auth.currentUser?.uid ?: return
        val profileData = hashMapOf(
            "uid" to uid,
            "fullName" to binding.nameEditText.text.toString(),
            "staffId" to binding.userIdEditText.text.toString(),
            "email" to binding.emailEditText.text.toString(),
            "department" to binding.departmentEditText.text.toString(),
            "designation" to binding.designationEditText.text.toString(),
            "officeLocation" to binding.officeLocationEditText.text.toString(),
            "mobile" to binding.contactNumberEditText.text.toString(),
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("admins").document(uid).set(profileData)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                // Upload profile image if selected
                profileImageUri?.let { uri -> uploadProfileImage(uri, uid) }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun selectProfileImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                profileImageUri = uri
                binding.profileImageView.setImageURI(uri)
            }
        }
    }

    private fun uploadProfileImage(uri: Uri, uid: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    db.collection("admins").document(uid)
                        .update("profileImageUrl", downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to upload profile image", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()

                val intent = Intent(requireContext(), GetIn::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
