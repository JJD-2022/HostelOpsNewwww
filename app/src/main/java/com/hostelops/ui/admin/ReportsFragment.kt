package com.hostelops.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.databinding.FragmentReportsBinding
import com.hostelops.models.Complaint
import java.util.concurrent.TimeUnit

class ReportsFragment : Fragment() {
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeReportData()
    }

    private fun observeReportData() {
        db.collection("complaints").addSnapshotListener { snapshot, _ ->
            val complaints = snapshot?.toObjects(Complaint::class.java) ?: emptyList()
            calculateResolutionTime(complaints)
            displayResolvedStats(complaints)
        }
    }

    private fun calculateResolutionTime(complaints: List<Complaint>) {
        val resolvedComplaints = complaints.filter { it.status == "RESOLVED" || it.status == "COMPLETED" }
        
        if (resolvedComplaints.isEmpty()) {
            binding.tvAvgResolutionTime.text = "No resolved cases yet"
            return
        }

        val timedComplaints = resolvedComplaints.filter { it.resolvedAt != null }
        
        if (timedComplaints.isEmpty()) {
            binding.tvAvgResolutionTime.text = "${resolvedComplaints.size} cases resolved"
            return
        }

        var totalTimeMillis: Long = 0
        timedComplaints.forEach { complaint ->
            val diff = complaint.resolvedAt!!.toDate().time - complaint.timestamp.toDate().time
            totalTimeMillis += diff
        }

        val avgMillis = totalTimeMillis / timedComplaints.size
        val hours = TimeUnit.MILLISECONDS.toHours(avgMillis)
        val days = TimeUnit.MILLISECONDS.toDays(avgMillis)

        binding.tvAvgResolutionTime.text = if (days > 0) "$days days, ${hours % 24} hours" else "$hours hours"
    }
    
    private fun displayResolvedStats(complaints: List<Complaint>) {
        val resolvedByCategory = complaints.filter { it.status == "RESOLVED" || it.status == "COMPLETED" }
            .groupBy { it.category }
            .mapValues { it.value.size }
            
        if (_binding == null || context == null) return

        binding.layoutResolvedStats.removeAllViews()
        if (resolvedByCategory.isEmpty()) {
            val emptyView = TextView(requireContext())
            emptyView.text = "No categories resolved yet"
            binding.layoutResolvedStats.addView(emptyView)
            return
        }

        resolvedByCategory.forEach { (category, count) ->
            val itemView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, binding.layoutResolvedStats, false)
            (itemView.findViewById<View>(android.R.id.text1) as TextView).text = "$category: $count cases"
            binding.layoutResolvedStats.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
