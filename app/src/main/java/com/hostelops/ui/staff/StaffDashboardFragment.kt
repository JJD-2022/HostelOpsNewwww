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
import com.google.firebase.auth.FirebaseAuth
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
    private lateinit var auth: FirebaseAuth
    private var currentFilter = "ALL"
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffDashboardBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvStaffComplaints.layoutManager = LinearLayoutManager(context)
        
        setupStaffGreeting()
        setupStaffQuote()
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

    private fun setupStaffGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..15 -> "Good Afternoon"
            else -> "Good Evening"
        }
        
        val user = auth.currentUser
        val name = user?.displayName?.split(" ")?.get(0) ?: "Staff"
        if (_binding != null) {
            binding.tvStaffGreeting.text = "$greeting, $name!"
        }
        
        db.collection("users").document(user?.uid ?: "").get().addOnSuccessListener { 
            if (_binding == null) return@addOnSuccessListener
            val firestoreName = it.getString("name")?.split(" ")?.get(0)
            if (firestoreName != null) {
                binding.tvStaffGreeting.text = "$greeting, $firestoreName!"
            }
        }
    }

    private fun setupStaffQuote() {
        val quotes = arrayOf(
            "\"Efficiency is doing things right; effectiveness is doing the right things.\"",
            "\"Quality is not an act, it is a habit.\"",
            "\"Good service is good business.\"",
            "\"Focus on being productive instead of busy.\"",
            "\"The way to achieve vertical success is to be horizontally focused.\""
        )
        if (_binding != null) {
            binding.tvStaffQuote.text = quotes.random()
        }
    }

    private fun loadComplaints() {
        db.collection("complaints").addSnapshotListener { value, error ->
            if (_binding == null || error != null) return@addSnapshotListener
            val allComplaints = value?.toObjects(Complaint::class.java) ?: emptyList()
            
            // Update Stats
            binding.tvNewTasks.text = allComplaints.count { it.status == "NOT_SEEN" }.toString()
            binding.tvActiveTasks.text = allComplaints.count { it.status == "IN_PROGRESS" }.toString()
            binding.tvCompletedTasks.text = allComplaints.count { it.status == "RESOLVED" || it.status == "COMPLETED" }.toString()
            
            // Apply Filters for List
            var filteredList = allComplaints
            
            if (currentFilter != "ALL" && currentFilter != "ESCALATED") {
                filteredList = filteredList.filter { it.status == currentFilter }
            } else if (currentFilter == "ESCALATED") {
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
                filteredList = filteredList.filter { 
                    it.status != "RESOLVED" && it.status != "COMPLETED" && it.timestamp.toDate().before(yesterday)
                }
            }

            if (searchQuery.isNotEmpty()) {
                filteredList = filteredList.filter { 
                    it.category.lowercase().contains(searchQuery) || 
                    it.block.lowercase().contains(searchQuery) ||
                    it.roomNo.lowercase().contains(searchQuery) ||
                    it.studentName.lowercase().contains(searchQuery)
                }
            }
            
            // Sort: Escalated (older than 24h) first
            val sortedList = filteredList.sortedWith(compareByDescending<Complaint> {
                it.status != "RESOLVED" && it.status != "COMPLETED" && (System.currentTimeMillis() - it.timestamp.toDate().time) > 86400000
            }.thenByDescending { it.timestamp })

            if (sortedList.isEmpty()) {
                binding.tvStaffEmptyState.visibility = View.VISIBLE
                binding.rvStaffComplaints.visibility = View.GONE
            } else {
                binding.tvStaffEmptyState.visibility = View.GONE
                binding.rvStaffComplaints.visibility = View.VISIBLE
            }

            binding.rvStaffComplaints.adapter = ComplaintAdapter(sortedList) { complaint ->
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
