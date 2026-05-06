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
    }

    private fun loadStats() {
        db.collection("complaints").get()
            .addOnSuccessListener { querySnapshot ->
                val total = querySnapshot.size()
                val resolved = querySnapshot.documents.count { it.getString("status") == "RESOLVED" }
                
                binding.tvTotalComplaints.text = total.toString()
                binding.tvResolvedComplaints.text = resolved.toString()
            }
    }

    private fun loadRecentComplaints() {
        db.collection("complaints")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val complaints = value?.toObjects(Complaint::class.java) ?: emptyList()
                binding.rvAdminRecent.adapter = ComplaintAdapter(complaints) { complaint ->
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
