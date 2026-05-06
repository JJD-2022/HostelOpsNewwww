package com.hostelops.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.R
import com.hostelops.databinding.FragmentProfileBinding
import com.hostelops.utils.AvatarUtils
import com.hostelops.utils.CloudinaryHelper
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadProfileImage(it) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let { uploadProfileImage(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        loadUserData()

        binding.btnUpdateProfile.setOnClickListener {
            updateUserProfile()
        }

        binding.btnChangePhotoGallery.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnChangePhotoCamera.setOnClickListener {
            val photoFile = File(requireContext().cacheDir, "profile_${System.currentTimeMillis()}.jpg")
            imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
            takePhoto.launch(imageUri)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_global_roleSelectionFragment)
        }
    }

    private fun setupSpinners() {
        val blocks = arrayOf("N Block", "Q Block", "R Block", "S Block", "T Block")
        binding.etProfileBlock.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, blocks))
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: user.displayName ?: ""
                    val role = document.getString("role") ?: ""
                    val block = document.getString("block") ?: ""
                    val roomNo = document.getString("roomNo") ?: ""
                    val email = document.getString("email") ?: user.email ?: ""
                    val rollNo = document.getString("rollNo") ?: ""
                    val phone = document.getString("phone") ?: user.phoneNumber ?: ""
                    val address = document.getString("address") ?: ""

                    binding.etProfileName.setText(name)
                    binding.etProfileEmail.setText(email)
                    binding.tvUserRole.text = role
                    binding.etProfileRollNo.setText(rollNo)
                    binding.etProfilePhone.setText(phone)
                    binding.etProfileAddress.setText(address)
                    
                    if (role == "STUDENT") {
                        binding.tilProfileRollNo.hint = "Roll No"
                        binding.layoutPhotoActions.visibility = View.VISIBLE
                        binding.layoutStudentHostel.visibility = View.VISIBLE
                        binding.etProfileBlock.setText(block, false)
                        binding.etProfileRoom.setText(roomNo)
                    } else {
                        binding.tilProfileRollNo.hint = "Employee ID"
                        binding.layoutPhotoActions.visibility = View.GONE
                        binding.layoutStudentHostel.visibility = View.GONE
                    }

                    val photoUrl = document.getString("photoUrl") ?: user.photoUrl?.toString()
                    if (photoUrl != null) {
                        binding.ivProfileDetail.load(photoUrl) {
                            crossfade(true)
                            placeholder(AvatarUtils.getLetterAvatar(requireContext(), name))
                            transformations(CircleCropTransformation())
                        }
                    } else {
                        binding.ivProfileDetail.setImageDrawable(AvatarUtils.getLetterAvatar(requireContext(), name))
                    }
                }
            }
    }

    private fun updateUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        val name = binding.etProfileName.text.toString().trim()
        val phone = binding.etProfilePhone.text.toString().trim()
        val rollNo = binding.etProfileRollNo.text.toString().trim()
        val address = binding.etProfileAddress.text.toString().trim()
        val block = binding.etProfileBlock.text.toString().trim()
        val roomNo = binding.etProfileRoom.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "rollNo" to rollNo,
            "address" to address
        )

        // Only add student fields if they are visible
        if (binding.layoutStudentHostel.visibility == View.VISIBLE) {
            updates["block"] = block
            updates["roomNo"] = roomNo
        }

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                loadUserData() // Refresh UI
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadProfileImage(uri: Uri) {
        CloudinaryHelper.uploadImage(requireContext(), uri, object : UploadCallback {
            override fun onStart(requestId: String) {}
            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                val url = resultData["secure_url"] as String
                saveProfilePhotoUrl(url)
            }
            override fun onError(requestId: String, error: ErrorInfo) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onReschedule(requestId: String, error: ErrorInfo) {}
        })
    }

    private fun saveProfilePhotoUrl(url: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("photoUrl", url)
            .addOnSuccessListener {
                binding.ivProfileDetail.load(url) {
                    transformations(CircleCropTransformation())
                }
                Toast.makeText(context, "Photo updated", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
