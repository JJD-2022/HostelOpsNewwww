package com.hostelops.ui.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hostelops.R
import com.hostelops.databinding.ItemComplaintBinding
import com.hostelops.models.Complaint
import java.text.SimpleDateFormat
import java.util.Locale

class ComplaintAdapter(private val complaints: List<Complaint>) :
    RecyclerView.Adapter<ComplaintAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemComplaintBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemComplaintBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val complaint = complaints[position]
        holder.binding.tvCategory.text = complaint.category
        holder.binding.tvDescription.text = complaint.description
        holder.binding.tvLocation.text = complaint.location
        holder.binding.tvStatus.text = complaint.status
        
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.binding.tvTimestamp.text = sdf.format(complaint.timestamp.toDate())

        when (complaint.status) {
            "NOT_SEEN" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_not_seen)
            "IN_PROGRESS" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
            "RESOLVED" -> holder.binding.tvStatus.setBackgroundResource(R.drawable.bg_status_resolved)
        }
    }

    override fun getItemCount() = complaints.size
}
