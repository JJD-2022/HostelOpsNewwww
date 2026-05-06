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
        val resolvedComplaints = complaints.filter { (it.status == "RESOLVED" || it.status == "COMPLETED") && it.resolvedAt != null }
        
        if (resolvedComplaints.isEmpty()) {
            binding.tvAvgResolutionTime.text = "No resolved cases yet"
            return
        }

        var totalTimeMillis: Long = 0
        resolvedComplaints.forEach { complaint ->
            val diff = complaint.resolvedAt!!.toDate().time - complaint.timestamp.toDate().time
            totalTimeMillis += diff
        }

        val avgMillis = totalTimeMillis / resolvedComplaints.size
        val hours = TimeUnit.MILLISECONDS.toHours(avgMillis)
        val days = TimeUnit.MILLISECONDS.toDays(avgMillis)

        binding.tvAvgResolutionTime.text = if (days > 0) "$days days, ${hours % 24} hours" else "$hours hours"
    }
    
    private fun displayResolvedStats(complaints: List<Complaint>) {
        val resolvedByCategory = complaints.filter { it.status == "RESOLVED" || it.status == "COMPLETED" }
            .groupBy { it.category }
            .mapValues { it.value.size }
            
        binding.layoutResolvedStats.removeAllViews()
        resolvedByCategory.forEach { (category, count) ->
            val itemView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, binding.layoutResolvedStats, false)
            (itemView.findViewById<View>(android.R.id.text1) as TextView).text = "$category: $count resolved"
            binding.layoutResolvedStats.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
