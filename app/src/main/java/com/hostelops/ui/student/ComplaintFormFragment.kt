package com.hostelops.ui.student

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
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.databinding.FragmentComplaintFormBinding
import com.hostelops.models.Complaint
import java.io.File

class ComplaintFormFragment : Fragment() {

    private var _binding: FragmentComplaintFormBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable("image_uri")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("image_uri", imageUri)
        // Save text details
        outState.putString("category", binding.spinnerCategory.text.toString())
        outState.putString("block", binding.spinnerBlock.text.toString())
        outState.putString("roomNo", binding.etRoomNo.text.toString())
        outState.putString("description", binding.etDescription.text.toString())
    }
    
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            binding.ivComplaintPhoto.setImageURI(uri)
            binding.cardPhoto.visibility = View.VISIBLE
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.ivComplaintPhoto.setImageURI(imageUri)
            binding.cardPhoto.visibility = View.VISIBLE
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
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

        // Restore image and text if exists
        savedInstanceState?.let { bundle ->
            imageUri = bundle.getParcelable("image_uri")
            binding.spinnerCategory.setText(bundle.getString("category"), false)
            binding.spinnerBlock.setText(bundle.getString("block"), false)
            binding.etRoomNo.setText(bundle.getString("roomNo"))
            binding.etDescription.setText(bundle.getString("description"))
        }

        imageUri?.let {
            binding.ivComplaintPhoto.setImageURI(it)
            binding.cardPhoto.visibility = View.VISIBLE
        }

        val categories = arrayOf("Plumbing", "Electrical", "Carpentry", "Cleaning", "Others")
        binding.spinnerCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))

        val blocks = arrayOf("N Block", "Q Block", "R Block", "S Block", "T Block")
        binding.spinnerBlock.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, blocks))

        binding.btnGallery.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission.launch(android.Manifest.permission.CAMERA)
            } else {
                launchCamera()
            }
        }

        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File(requireContext().getExternalFilesDir(null), "temp_photo_${System.currentTimeMillis()}.jpg")
            imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
            takePhoto.launch(imageUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not start camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun validateAndSubmit() {
        val category = binding.spinnerCategory.text.toString()
        val block = binding.spinnerBlock.text.toString()
        val roomNo = binding.etRoomNo.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (category.isEmpty() || block.isEmpty() || roomNo.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false

        // Fetch current user details first to ensure the complaint has all info
        val uid = auth.currentUser?.uid ?: return

        // Duplicate detection logic
        db.collection("complaints")
            .whereEqualTo("studentId", uid)
            .whereEqualTo("category", category)
            .whereEqualTo("description", description)
            .get()
            .addOnSuccessListener { documents ->
                var isDayDuplicate = false
                var isFiveDayDuplicate = false
                val now = System.currentTimeMillis()
                val oneDayMillis = 24 * 60 * 60 * 1000L
                val fiveDaysMillis = 5 * oneDayMillis

                for (doc in documents) {
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    val diff = now - timestamp
                    if (diff < oneDayMillis) {
                        isDayDuplicate = true
                        break
                    } else if (diff < fiveDaysMillis) {
                        isFiveDayDuplicate = true
                    }
                }

                if (isDayDuplicate) {
                    Toast.makeText(context, "Duplicate complaint detected! You have already filed this complaint today. Please avoid redundant entries.", Toast.LENGTH_LONG).show()
                    binding.btnSubmit.isEnabled = true
                } else {
                    proceedWithSubmission(category, block, roomNo, description, uid, isFiveDayDuplicate)
                }
            }
            .addOnFailureListener {
                // If check fails, we proceed without flagging to avoid blocking valid users
                proceedWithSubmission(category, block, roomNo, description, uid, false)
            }
    }

    private fun proceedWithSubmission(category: String, block: String, roomNo: String, description: String, uid: String, isDuplicate: Boolean) {
        db.collection("users").document(uid).get().addOnSuccessListener { document ->
            val name = document.getString("name") ?: "Unknown"
            val phone = document.getString("phone") ?: "N/A"
            val email = document.getString("email") ?: ""

            if (imageUri != null) {
                uploadImageAndSave(category, block, roomNo, description, name, phone, email, isDuplicate)
            } else {
                saveComplaintToFirestore(category, block, roomNo, description, name, phone, email, "", isDuplicate)
            }
        }.addOnFailureListener {
            binding.btnSubmit.isEnabled = true
            Toast.makeText(context, "Failed to fetch user details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageAndSave(category: String, block: String, roomNo: String, description: String, name: String, phone: String, email: String, isDuplicate: Boolean) {
        MediaManager.get().upload(imageUri)
            .unsigned("kkkae34a") 
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val uploadedPhotoUrl = resultData["secure_url"] as String
                    saveComplaintToFirestore(category, block, roomNo, description, name, phone, email, uploadedPhotoUrl, isDuplicate)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(context, "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun saveComplaintToFirestore(category: String, block: String, roomNo: String, description: String, name: String, phone: String, email: String, photoUrl: String, isDuplicate: Boolean) {
        val complaintId = db.collection("complaints").document().id
        
        val complaint = Complaint(
            id = complaintId,
            studentId = auth.currentUser?.uid ?: "",
            studentName = name,
            studentEmail = email,
            studentPhone = phone,
            category = category,
            block = block,
            roomNo = roomNo,
            description = description,
            photoUrl = photoUrl,
            isDuplicate = isDuplicate
        )

        db.collection("complaints").document(complaintId).set(complaint)
            .addOnSuccessListener {
                createNotificationForStaff(category, "$block - $roomNo")
                Toast.makeText(requireContext().applicationContext, "Complaint posted successfully!", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                binding.btnSubmit.isEnabled = true
                Toast.makeText(context, "Post failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNotificationForStaff(category: String, location: String) {
        val notification = hashMapOf(
            "title" to "New Complaint: $category",
            "message" to "A new complaint has been filed at $location",
            "targetRole" to "STAFF",
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        db.collection("notifications").add(notification)
        
        val adminNotification = notification.toMutableMap()
        adminNotification["targetRole"] = "ADMIN"
        db.collection("notifications").add(adminNotification)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
