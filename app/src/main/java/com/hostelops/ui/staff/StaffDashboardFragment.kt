package com.hostelops.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hostelops.databinding.FragmentStaffDashboardBinding
import com.hostelops.models.Complaint
import com.hostelops.ui.student.ComplaintAdapter

class StaffDashboardFragment : Fragment() {

    private var _binding: FragmentStaffDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore
    private var currentFilter = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffDashboardBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvStaffComplaints.layoutManager = LinearLayoutManager(context)
        loadComplaints()

        binding.chipGroupFilters.setOnCheckedChangeListener { group, checkedId ->
            currentFilter = when (checkedId) {
                binding.chipNotSeen.id -> "NOT_SEEN"
                binding.chipInProgress.id -> "IN_PROGRESS"
                else -> "ALL"
            }
            loadComplaints()
        }
    }

    private fun loadComplaints() {
        var query: Query = db.collection("complaints").orderBy("timestamp", Query.Direction.DESCENDING)
        
        if (currentFilter != "ALL") {
            query = query.whereEqualTo("status", currentFilter)
        }

        query.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            val complaints = value?.toObjects(Complaint::class.java) ?: emptyList()
            binding.rvStaffComplaints.adapter = ComplaintAdapter(complaints)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
