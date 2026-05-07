package com.hostelops.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.R
import com.hostelops.databinding.FragmentComplaintDetailBinding
import com.hostelops.models.Complaint
import java.text.SimpleDateFormat
import java.util.Locale

class ComplaintDetailFragment : Fragment() {

    private var _binding: FragmentComplaintDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ComplaintDetailFragmentArgs by navArgs()
    
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComplaintDetailBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadComplaintDetails(args.complaintId)
    }

    private fun loadComplaintDetails(complaintId: String) {
        db.collection("complaints").document(complaintId).addSnapshotListener { document, _ ->
            if (_binding == null) return@addSnapshotListener
            val complaint = document?.toObject(Complaint::class.java)
            if (complaint != null) {
                displayComplaint(complaint)
                checkUserRoleAndShowActions(complaint)
            }
        }
    }

    private fun displayComplaint(complaint: Complaint) {
        binding.tvCategory.text = complaint.category
        binding.tvDescription.text = complaint.description
        binding.tvLocation.text = "${complaint.block}, Room ${complaint.roomNo}"
        binding.tvStatus.text = complaint.status
        
        binding.tvDuplicateFlag.visibility = if (complaint.isDuplicate) View.VISIBLE else View.GONE

        binding.tvStudentName.text = complaint.studentName
        binding.tvStudentContact.text = "${complaint.studentEmail} | ${complaint.studentPhone}"
        
        val sdf = SimpleDateFormat("dd.MM.yyyy, h:mm a", Locale.getDefault())
        binding.tvTimestamp.text = "Submitted on: ${sdf.format(complaint.timestamp.toDate())}"

        if (complaint.photoUrl.isNotEmpty()) {
            binding.cardPhoto.visibility = View.VISIBLE
            binding.ivComplaintPhoto.load(complaint.photoUrl) {
                placeholder(R.drawable.ic_profile_placeholder)
                crossfade(true)
            }
        } else {
            binding.cardPhoto.visibility = View.GONE
        }

        if (complaint.status == "RESOLVED" || complaint.status == "COMPLETED") {
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_resolved)
            binding.layoutResolution.visibility = View.VISIBLE
            binding.tvStaffRemarks.text = if (complaint.staffRemarks.isEmpty()) "No remarks provided." else complaint.staffRemarks
            if (complaint.resolutionPhotoUrl.isNotEmpty()) {
                binding.ivResolutionPhoto.visibility = View.VISIBLE
                binding.ivResolutionPhoto.load(complaint.resolutionPhotoUrl)
            } else {
                binding.ivResolutionPhoto.visibility = View.GONE
            }
        } else if (complaint.status == "IN_PROGRESS") {
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
            binding.layoutResolution.visibility = View.GONE
        } else {
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_not_seen)
            binding.layoutResolution.visibility = View.GONE
        }
    }

    private fun checkUserRoleAndShowActions(complaint: Complaint) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                val role = document.getString("role")
                if (role == "STAFF" && complaint.status != "RESOLVED" && complaint.status != "COMPLETED") {
                    binding.layoutStaffActions.visibility = View.VISIBLE
                    setupStaffActions(complaint)
                } else {
                    binding.layoutStaffActions.visibility = View.GONE
                }
            }
    }

    private fun setupStaffActions(complaint: Complaint) {
        binding.btnInProgress.setOnClickListener {
            updateStatus(complaint.id, "IN_PROGRESS")
        }

        binding.btnResolve.setOnClickListener {
            showResolveDialog(complaint.id)
        }
    }

    private fun showResolveDialog(id: String) {
        val editText = EditText(requireContext())
        editText.hint = "Enter resolution remarks (optional)"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Resolve Complaint")
            .setView(editText)
            .setPositiveButton("Resolve") { _, _ ->
                val remarks = editText.text.toString().trim()
                updateStatus(id, "RESOLVED", remarks)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStatus(id: String, status: String, remarks: String = "") {
        val updates = mutableMapOf<String, Any>(
            "status" to status
        )
        if (status == "RESOLVED") {
            updates["staffRemarks"] = remarks
            updates["resolvedAt"] = com.google.firebase.Timestamp.now()
        }

        db.collection("complaints").document(id).update(updates)
            .addOnSuccessListener {
                if (_binding != null) {
                    Toast.makeText(requireContext().applicationContext, "Status updated to $status", Toast.LENGTH_SHORT).show()
                    createNotifications(id, status)
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    Toast.makeText(context, "Failed to update: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createNotifications(complaintId: String, status: String) {
        db.collection("complaints").document(complaintId).get()
            .addOnSuccessListener { document ->
                val studentId = document.getString("studentId") ?: return@addOnSuccessListener
                val category = document.getString("category") ?: "Complaint"
                
                // Student Notification
                val studentNotification = hashMapOf(
                    "title" to "Update: $category",
                    "message" to "Your complaint status is now: $status",
                    "targetUid" to studentId,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                db.collection("notifications").add(studentNotification)
                
                // Admin Notification
                val adminNotification = hashMapOf(
                    "title" to "Complaint $status: $category",
                    "message" to "A complaint status has been updated to $status by staff.",
                    "targetRole" to "ADMIN",
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                db.collection("notifications").add(adminNotification)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
