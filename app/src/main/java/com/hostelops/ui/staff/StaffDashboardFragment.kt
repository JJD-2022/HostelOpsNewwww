package com.hostelops.ui.staff

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hostelops.databinding.FragmentStaffDashboardBinding
import com.hostelops.models.Complaint
import com.hostelops.ui.student.ComplaintAdapter
import java.util.Calendar

class StaffDashboardFragment : Fragment() {

    private var _binding: FragmentStaffDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore
    private var currentFilter = "ALL"
    private var searchQuery = ""

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

        binding.chipGroupFilters.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                binding.chipNotSeen.id -> "NOT_SEEN"
                binding.chipInProgress.id -> "IN_PROGRESS"
                binding.chipEscalated.id -> "ESCALATED"
                else -> "ALL"
            }
            loadComplaints()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().lowercase()
                loadComplaints()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadComplaints() {
        var query: Query = db.collection("complaints").orderBy("timestamp", Query.Direction.DESCENDING)
        
        if (currentFilter != "ALL" && currentFilter != "ESCALATED") {
            query = query.whereEqualTo("status", currentFilter)
        }

        query.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            var complaints = value?.toObjects(Complaint::class.java) ?: emptyList()

            // Apply Escalation Filter
            if (currentFilter == "ESCALATED") {
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
                complaints = complaints.filter { 
                    it.status != "RESOLVED" && it.timestamp.toDate().before(yesterday)
                }
            }

            // Apply Search Filter
            if (searchQuery.isNotEmpty()) {
                complaints = complaints.filter { 
                    it.category.lowercase().contains(searchQuery) || 
                    it.block.lowercase().contains(searchQuery) ||
                    it.roomNo.lowercase().contains(searchQuery) ||
                    it.studentName.lowercase().contains(searchQuery) ||
                    it.studentEmail.lowercase().contains(searchQuery)
                }
            }
            
            // Priority Sorting: Escalated first, then by timestamp
            val sortedComplaints = complaints.sortedWith(compareByDescending<Complaint> {
                it.status != "RESOLVED" && (System.currentTimeMillis() - it.timestamp.toDate().time) > 86400000
            }.thenByDescending { it.timestamp })

            binding.rvStaffComplaints.adapter = ComplaintAdapter(sortedComplaints) { complaint ->
                val action = StaffDashboardFragmentDirections.actionStaffDashboardFragmentToComplaintDetailFragment(complaint.id)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
