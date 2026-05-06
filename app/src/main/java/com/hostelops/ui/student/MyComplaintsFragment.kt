package com.hostelops.ui.student

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hostelops.R
import com.hostelops.databinding.FragmentMyComplaintsBinding
import com.hostelops.models.Complaint

class MyComplaintsFragment : Fragment() {
    private var _binding: FragmentMyComplaintsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var allComplaints = listOf<Complaint>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyComplaintsBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.rvMyComplaints.layoutManager = LinearLayoutManager(context)
        loadComplaints()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterComplaints(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.fabAddComplaint.setOnClickListener {
            findNavController().navigate(R.id.action_myComplaintsFragment_to_complaintFormFragment)
        }
    }

    private fun loadComplaints() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("complaints")
            .whereEqualTo("studentId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                allComplaints = value?.toObjects(Complaint::class.java) ?: emptyList()
                filterComplaints(binding.etSearch.text.toString())
            }
    }

    private fun filterComplaints(query: String) {
        val filtered = if (query.isEmpty()) {
            allComplaints
        } else {
            allComplaints.filter { 
                it.category.lowercase().contains(query.lowercase()) || 
                it.description.lowercase().contains(query.lowercase()) ||
                it.status.lowercase().contains(query.lowercase())
            }
        }

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvMyComplaints.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvMyComplaints.visibility = View.VISIBLE
        }

        binding.rvMyComplaints.adapter = ComplaintAdapter(filtered) { complaint ->
            val action = MyComplaintsFragmentDirections.actionMyComplaintsFragmentToComplaintDetailFragment(complaint.id)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
