package com.hostelops.ui.student

import android.os.Bundle
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
import com.hostelops.databinding.FragmentStudentDashboardBinding
import com.hostelops.models.Complaint

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
        
        loadComplaints()

        binding.fabAddComplaint.setOnClickListener {
            findNavController().navigate(R.id.action_studentDashboardFragment_to_complaintFormFragment)
        }
    }

    private fun loadComplaints() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("complaints")
            .whereEqualTo("studentId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val complaints = value?.toObjects(Complaint::class.java) ?: emptyList()
                binding.rvComplaints.adapter = ComplaintAdapter(complaints)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
