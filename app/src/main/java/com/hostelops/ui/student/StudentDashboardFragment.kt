package com.hostelops.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hostelops.R
import com.hostelops.databinding.FragmentStudentDashboardBinding
import com.hostelops.models.Complaint
import java.util.Calendar

class StudentDashboardFragment : Fragment() {

    private var _binding: FragmentStudentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentDashboardBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvComplaints.layoutManager = LinearLayoutManager(context)
        
        setupGreeting()
        setupQuotes()
        checkProfileCompleteness()
        loadComplaints()

        binding.fabAddComplaint.setOnClickListener {
            findNavController().navigate(R.id.action_studentDashboardFragment_to_complaintFormFragment)
        }
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..15 -> "Good Afternoon"
            else -> "Good Evening"
        }
        
        val user = auth.currentUser
        val name = user?.displayName?.split(" ")?.get(0) ?: "Student"
        binding.tvGreeting.text = "$greeting, $name!"
        
        // Try to get name from Firestore for better accuracy
        db.collection("users").document(user?.uid ?: "").get().addOnSuccessListener { 
            val firestoreName = it.getString("name")?.split(" ")?.get(0)
            if (firestoreName != null) {
                binding.tvGreeting.text = "$greeting, $firestoreName!"
            }
        }
    }

    private fun setupQuotes() {
        val quotes = arrayOf(
            "\"The secret of getting ahead is getting started.\"",
            "\"Everything is possible. The impossible just takes longer.\"",
            "\"Don't count the days, make the days count.\"",
            "\"Happiness is not something ready made. It comes from your own actions.\"",
            "\"Success is not final, failure is not fatal: it is the courage to continue that counts.\""
        )
        binding.tvQuote.text = quotes.random()
    }

    private fun checkProfileCompleteness() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { document ->
            val block = document.getString("block") ?: ""
            val roomNo = document.getString("roomNo") ?: ""
            
            if (block.isEmpty() || roomNo.isEmpty()) {
                showIncompleteProfileDialog()
            }
        }
    }

    private fun showIncompleteProfileDialog() {
        if (context == null) return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Incomplete")
            .setMessage("Please update your Hostel Block and Room Number in your profile to continue.")
            .setPositiveButton("Go to Profile") { _, _ ->
                findNavController().navigate(R.id.profileFragment)
            }
            .setCancelable(false)
            .show()
    }

    private fun loadComplaints() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("complaints")
            .whereEqualTo("studentId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val complaints = value?.toObjects(Complaint::class.java) ?: emptyList()
                
                binding.tvTotalComplaints.text = complaints.size.toString()
                binding.tvResolvedComplaints.text = complaints.count { it.status == "RESOLVED" || it.status == "COMPLETED" }.toString()
                
                if (complaints.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvComplaints.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvComplaints.visibility = View.VISIBLE
                }

                binding.rvComplaints.adapter = ComplaintAdapter(complaints) { complaint ->
                    val action = StudentDashboardFragmentDirections.actionStudentDashboardFragmentToComplaintDetailFragment(complaint.id)
                    findNavController().navigate(action)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
