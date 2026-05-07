package com.hostelops.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hostelops.R
import com.hostelops.databinding.FragmentAdminDashboardBinding
import com.hostelops.models.Complaint
import com.hostelops.ui.student.ComplaintAdapter

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAdminRecent.layoutManager = LinearLayoutManager(context)
        loadStats()
        loadRecentComplaints()
        
        binding.cardManageUsers.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminUsersFragment)
        }

        binding.cardAnalytics.setOnClickListener {
            findNavController().navigate(R.id.analyticsFragment)
        }

        binding.cardReports.setOnClickListener {
            findNavController().navigate(R.id.reportsFragment)
        }
    }

    private fun loadStats() {
        db.collection("complaints").addSnapshotListener { querySnapshot, _ ->
            if (querySnapshot == null) return@addSnapshotListener
            
            val total = querySnapshot.size()
            val resolved = querySnapshot.documents.count { it.getString("status") == "RESOLVED" }
            val inProgress = querySnapshot.documents.count { it.getString("status") == "IN_PROGRESS" }
            val pending = total - resolved - inProgress
            
            binding.tvTotalComplaints.text = total.toString()
            binding.tvResolvedComplaints.text = resolved.toString()
            binding.tvInProgress.text = inProgress.toString()
            binding.tvPendingComplaints.text = pending.toString()
        }
    }

    private fun loadRecentComplaints() {
        db.collection("complaints")
            .limit(50)
            .addSnapshotListener { value, error ->
                if (_binding == null || error != null) return@addSnapshotListener
                val complaints = value?.toObjects(Complaint::class.java) ?: emptyList()
                
                // Sort in memory to avoid index requirement
                val sorted = complaints.sortedByDescending { it.timestamp }.take(15)
                
                binding.rvAdminRecent.adapter = ComplaintAdapter(sorted) { complaint ->
                    val action = AdminDashboardFragmentDirections.actionAdminDashboardFragmentToComplaintDetailFragment(complaint.id)
                    findNavController().navigate(action)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
