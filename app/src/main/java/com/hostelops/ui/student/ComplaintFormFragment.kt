package com.hostelops.ui.student

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.databinding.FragmentComplaintFormBinding
import com.hostelops.models.Complaint

class ComplaintFormFragment : Fragment() {

    private var _binding: FragmentComplaintFormBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            binding.ivComplaintPhoto.setImageURI(uri)
        }
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComplaintFormBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categories = arrayOf("Plumbing", "Electrical", "Carpentry", "Cleaning", "Others")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)

        binding.cardPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSubmit.setOnClickListener {
            submitComplaint()
        }
    }

    private fun submitComplaint() {
        val category = binding.spinnerCategory.text.toString()
        val location = binding.etLocation.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (category.isEmpty() || location.isEmpty() || description.isEmpty() || imageUri == null) {
            Toast.makeText(context, "Please fill all fields and upload photo", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false

        // Cloudinary Unsigned Upload
        MediaManager.get().upload(imageUri)
            .unsigned("kkkae34a") // TODO: Replace with your Cloudinary Unsigned Upload Preset
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val photoUrl = resultData["secure_url"] as String
                    saveComplaintToFirestore(category, location, description, photoUrl)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(context, "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun saveComplaintToFirestore(category: String, location: String, description: String, photoUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        val complaintId = db.collection("complaints").document().id
        
        val complaint = Complaint(
            id = complaintId,
            studentId = uid,
            studentEmail = email,
            category = category,
            location = location,
            description = description,
            photoUrl = photoUrl
        )

        db.collection("complaints").document(complaintId).set(complaint)
            .addOnSuccessListener {
                Toast.makeText(context, "Complaint submitted successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                binding.btnSubmit.isEnabled = true
                Toast.makeText(context, "Error saving complaint", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
