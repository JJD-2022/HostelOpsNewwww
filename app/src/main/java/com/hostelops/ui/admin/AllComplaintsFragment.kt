package com.hostelops.ui.admin

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
import com.hostelops.databinding.FragmentComplaintListBinding
import com.hostelops.models.Complaint
import com.hostelops.ui.student.ComplaintAdapter

class AllComplaintsFragment : Fragment() {
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
        db.collection("complaints")
            .addSnapshotListener { value, error ->
                if (_binding == null || error != null) return@addSnapshotListener
                val complaints = value?.toObjects(Complaint::class.java) ?: emptyList()
                allComplaints = complaints.sortedByDescending { it.timestamp }
                filterAndDisplay(binding.etSearch.text.toString())
            }
    }

    private fun filterAndDisplay(query: String) {
        val filtered = if (query.isEmpty()) {
            allComplaints
        } else {
            allComplaints.filter {
                it.category.lowercase().contains(query.lowercase()) ||
                it.studentName.lowercase().contains(query.lowercase()) ||
                it.block.lowercase().contains(query.lowercase()) ||
                it.status.lowercase().contains(query.lowercase())
            }
        }

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvComplaints.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvComplaints.visibility = View.VISIBLE
            binding.rvComplaints.adapter = ComplaintAdapter(filtered) { complaint ->
                val action = AllComplaintsFragmentDirections.actionAllComplaintsFragmentToComplaintDetailFragment(complaint.id)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
