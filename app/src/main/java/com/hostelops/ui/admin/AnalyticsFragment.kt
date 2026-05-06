package com.hostelops.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hostelops.databinding.FragmentAnalyticsBinding
import com.hostelops.models.Complaint
import com.hostelops.models.User

class AnalyticsFragment : Fragment() {
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeData()
    }

    private fun observeData() {
        db.collection("complaints").addSnapshotListener { snapshot, _ ->
            val complaints = snapshot?.toObjects(Complaint::class.java) ?: emptyList()
            calculateComplaintStats(complaints)
        }

        db.collection("users").addSnapshotListener { snapshot, _ ->
            val users = snapshot?.toObjects(User::class.java) ?: emptyList()
            binding.tvStudentCount.text = users.count { it.role == "STUDENT" }.toString()
            binding.tvStaffCount.text = users.count { it.role == "STAFF" }.toString()
        }
    }

    private fun calculateComplaintStats(complaints: List<Complaint>) {
        if (complaints.isEmpty()) {
            binding.tvResolutionRate.text = "0%"
            binding.progressResolution.progress = 0
            return
        }

        val resolved = complaints.count { it.status == "RESOLVED" || it.status == "COMPLETED" }
        val rate = (resolved.toFloat() / complaints.size * 100).toInt()
        
        binding.tvResolutionRate.text = "$rate%"
        binding.progressResolution.progress = rate

        // Category distribution
        val categoryCounts = complaints.groupBy { it.category }.mapValues { it.value.size }
        binding.layoutCategoryStats.removeAllViews()
        
        categoryCounts.forEach { (category, count) ->
            val itemView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, binding.layoutCategoryStats, false)
            (itemView.findViewById<View>(android.R.id.text1) as TextView).text = "$category: $count"
            binding.layoutCategoryStats.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
