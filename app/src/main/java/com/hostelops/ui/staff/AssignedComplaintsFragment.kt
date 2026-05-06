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
import com.hostelops.databinding.FragmentComplaintListBinding
import com.hostelops.models.Complaint
import com.hostelops.ui.student.ComplaintAdapter

class AssignedComplaintsFragment : Fragment() {
    private var _binding: FragmentComplaintListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore
    private var allComplaints = listOf<Complaint>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentComplaintListBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvComplaints.layoutManager = LinearLayoutManager(context)
        loadComplaints()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (_binding != null) filterAndDisplay(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadComplaints() {
        // Remove orderBy timestamp to avoid Index required error, sort in memory instead
        db.collection("complaints")
            .whereEqualTo("status", "NOT_SEEN")
            .addSnapshotListener { value, error ->
                if (_binding == null || error != null) return@addSnapshotListener
                val complaints = value?.toObjects(Complaint::class.java) ?: emptyList()
                // Sort in memory
                allComplaints = complaints.sortedByDescending { it.timestamp }
                filterAndDisplay(binding.etSearch.text.toString())
            }
    }

    private fun filterAndDisplay(query: String) {
        val filtered = allComplaints.filter {
            it.category.lowercase().contains(query.lowercase()) ||
            it.studentName.lowercase().contains(query.lowercase()) ||
            it.block.lowercase().contains(query.lowercase())
        }

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvComplaints.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvComplaints.visibility = View.VISIBLE
            binding.rvComplaints.adapter = ComplaintAdapter(filtered) { complaint ->
                val action = AssignedComplaintsFragmentDirections.actionAssignedComplaintsFragmentToComplaintDetailFragment(complaint.id)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
