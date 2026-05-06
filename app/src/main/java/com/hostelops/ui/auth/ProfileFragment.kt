package com.hostelops.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.R
import com.hostelops.databinding.FragmentProfileBinding
import com.hostelops.utils.AvatarUtils
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

        loadUserData()

        binding.btnUpdateAddress.setOnClickListener {
            updateAddress()
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

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: user.displayName ?: "User"
                    binding.tvUserName.text = name
                    binding.tvUserEmail.text = document.getString("email") ?: user.email
                    binding.tvUserRole.text = document.getString("role")
                    binding.tvRollNo.text = document.getString("rollNo") ?: "N/A"
                    binding.tvPhone.text = document.getString("phone") ?: user.phoneNumber ?: "N/A"
                    binding.etProfileAddress.setText(document.getString("address") ?: "")
                    
                    if (document.getString("role") != "STUDENT") {
                        binding.tvRollNoLabel.text = "Employee ID"
                        binding.layoutPhotoActions.visibility = View.GONE
                    } else {
                        binding.layoutPhotoActions.visibility = View.VISIBLE
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

    private fun updateAddress() {
        val uid = auth.currentUser?.uid ?: return
        val newAddress = binding.etProfileAddress.text.toString().trim()

        binding.btnUpdateAddress.isEnabled = false
        db.collection("users").document(uid).update("address", newAddress)
            .addOnSuccessListener {
                binding.btnUpdateAddress.isEnabled = true
                Toast.makeText(context, "Address updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.btnUpdateAddress.isEnabled = true
                Toast.makeText(context, "Failed to update address", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadProfileImage(uri: Uri) {
        Toast.makeText(context, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
        MediaManager.get().upload(uri)
            .unsigned("kkkae34a") 
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val photoUrl = resultData["secure_url"] as String
                    saveProfilePhotoUrl(photoUrl)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    Toast.makeText(context, "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun saveProfilePhotoUrl(url: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("photoUrl", url)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                loadUserData()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
