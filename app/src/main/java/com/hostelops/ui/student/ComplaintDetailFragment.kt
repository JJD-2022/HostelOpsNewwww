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
        db.collection("complaints").document(complaintId).get()
            .addOnSuccessListener { document ->
                val complaint = document.toObject(Complaint::class.java)
                if (complaint != null) {
                    displayComplaint(complaint)
                    checkUserRoleAndShowActions(complaint)
                }
            }
    }

    private fun displayComplaint(complaint: Complaint) {
        binding.tvCategory.text = complaint.category
        binding.tvDescription.text = complaint.description
        binding.tvLocation.text = complaint.location
        binding.tvStatus.text = complaint.status
        
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        binding.tvTimestamp.text = "Submitted on: ${sdf.format(complaint.timestamp.toDate())}"

        binding.ivComplaintPhoto.load(complaint.photoUrl) {
            placeholder(R.drawable.ic_profile_placeholder)
            crossfade(true)
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
            // For now, just mark resolved. In a real app, we'd open a dialog to upload a photo.
            updateStatus(complaint.id, "RESOLVED")
        }
    }

    private fun updateStatus(id: String, status: String) {
        db.collection("complaints").document(id).update("status", status)
            .addOnSuccessListener {
                Toast.makeText(context, "Status updated to $status", Toast.LENGTH_SHORT).show()
                loadComplaintDetails(id)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
