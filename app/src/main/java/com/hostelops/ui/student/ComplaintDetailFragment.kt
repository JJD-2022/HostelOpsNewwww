package com.hostelops.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        when (complaint.status) {
            "NOT_SEEN" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_not_seen)
            "IN_PROGRESS" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
            "RESOLVED" -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_resolved)
                binding.layoutResolution.visibility = View.VISIBLE
                binding.tvStaffRemarks.text = complaint.staffRemarks
                binding.ivResolutionPhoto.load(complaint.resolutionPhotoUrl)
            }
        }
    }

    private fun checkUserRoleAndShowActions(complaint: Complaint) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                if (role == "STAFF" && complaint.status != "RESOLVED") {
                    binding.layoutStaffActions.visibility = View.VISIBLE
                    setupStaffActions(complaint)
                }
            }
    }

    private fun setupStaffActions(complaint: Complaint) {
        binding.btnInProgress.setOnClickListener {
            updateStatus(complaint.id, "IN_PROGRESS")
        }

        binding.btnResolve.setOnClickListener {
            updateStatus(complaint.id, "RESOLVED")
        }
    }

    private fun updateStatus(id: String, status: String) {
        db.collection("complaints").document(id).update("status", status)
            .addOnSuccessListener {
                Toast.makeText(context, "Status updated to $status", Toast.LENGTH_SHORT).show()
                createNotificationForStudent(id, status)
            }
    }

    private fun createNotificationForStudent(complaintId: String, status: String) {
        db.collection("complaints").document(complaintId).get()
            .addOnSuccessListener { document ->
                val studentId = document.getString("studentId") ?: return@addOnSuccessListener
                val category = document.getString("category") ?: "Complaint"
                
                val notification = hashMapOf(
                    "title" to "Update: $category",
                    "message" to "Your complaint status is now: $status",
                    "targetUid" to studentId,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                db.collection("notifications").add(notification)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
