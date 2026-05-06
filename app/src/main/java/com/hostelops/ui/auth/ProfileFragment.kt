package com.hostelops.ui.auth

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable("image_uri")
            isEditMode = savedInstanceState.getBoolean("is_edit_mode")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("image_uri", imageUri)
        outState.putBoolean("is_edit_mode", isEditMode)
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadProfileImage(it) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let { uploadProfileImage(it) }
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
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
        
        // Restore edit mode state
        if (isEditMode) {
            toggleEditMode(true)
        }

        binding.btnEditProfile.setOnClickListener {
            toggleEditMode(!isEditMode)
        }

        binding.btnUpdateProfile.setOnClickListener {
            updateUserProfile()
        }

        binding.btnChangePhotoGallery.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnChangePhotoCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission.launch(android.Manifest.permission.CAMERA)
            } else {
                launchCamera()
            }
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

    private fun toggleEditMode(enable: Boolean) {
        isEditMode = enable
        
        binding.tilProfileName.isEnabled = enable
        binding.tilProfileRollNo.isEnabled = enable
        binding.tilProfilePhone.isEnabled = enable
        binding.tilProfileAddress.isEnabled = enable
        binding.tilProfileBlock.isEnabled = enable
        binding.tilProfileRoom.isEnabled = enable
        
        binding.btnUpdateProfile.visibility = if (enable) View.VISIBLE else View.GONE
        binding.btnEditProfile.text = if (enable) "Cancel Editing" else "Edit Details"
        binding.btnEditProfile.setIconResource(if (enable) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_menu_edit)
    }

    private fun launchCamera() {
        try {
            val photoFile = File(requireContext().getExternalFilesDir(null), "profile_${System.currentTimeMillis()}.jpg")
            imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
            takePhoto.launch(imageUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not start camera: ${e.message}", Toast.LENGTH_LONG).show()
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

        if (binding.layoutStudentHostel.visibility == View.VISIBLE) {
            updates["block"] = block
            updates["roomNo"] = roomNo
        }

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                toggleEditMode(false)
                loadUserData()
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
