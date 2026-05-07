package com.hostelops.ui.student

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hostelops.R
import com.hostelops.databinding.ItemComplaintBinding
import com.hostelops.models.Complaint
import java.text.SimpleDateFormat
import java.util.Locale

class ComplaintAdapter(
    private val complaints: List<Complaint>,
    private val onComplaintClick: (Complaint) -> Unit
) : RecyclerView.Adapter<ComplaintAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemComplaintBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemComplaintBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val complaint = complaints[position]
        holder.binding.tvCategory.text = complaint.category
        holder.binding.tvDescription.text = complaint.description
        holder.binding.tvLocation.text = "${complaint.block} - ${complaint.roomNo}"
        holder.binding.tvStatus.text = complaint.status
        
        holder.binding.tvStudentInfo.text = "By: ${complaint.studentName} (${complaint.studentPhone})"
        
        holder.binding.tvDuplicate.visibility = if (complaint.isDuplicate) View.VISIBLE else View.GONE

        // Priority/Escalation Highlighting
        val isEscalated = complaint.status != "RESOLVED" && 
                (System.currentTimeMillis() - complaint.timestamp.toDate().time) > 86400000 // 24 hours
        
        if (isEscalated) {
            holder.binding.tvStatus.text = "URGENT / ESCALATED"
            holder.binding.cardComplaint.strokeColor = holder.binding.cardComplaint.context.getColor(R.color.md_theme_error)
            holder.binding.cardComplaint.strokeWidth = 6
        } else {
            holder.binding.cardComplaint.strokeColor = holder.binding.cardComplaint.context.getColor(R.color.md_theme_surfaceVariant)
            holder.binding.cardComplaint.strokeWidth = 2
        }

        val sdf = SimpleDateFormat("dd.MM.yyyy, h:mm a", Locale.getDefault())
        holder.binding.tvTimestamp.text = sdf.format(complaint.timestamp.toDate())

        when (complaint.status) {
            "NOT_SEEN" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_not_seen)
            "IN_PROGRESS" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
            "RESOLVED" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_resolved)
        }

        holder.itemView.setOnClickListener {
            onComplaintClick(complaint)
        }
    }

    override fun getItemCount() = complaints.size
}
