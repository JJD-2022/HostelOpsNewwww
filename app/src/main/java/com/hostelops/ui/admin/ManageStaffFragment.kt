package com.hostelops.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.databinding.FragmentManageStaffBinding
import com.hostelops.models.User

class ManageStaffFragment : Fragment() {
    private var _binding: FragmentManageStaffBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageStaffBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvStaffList.layoutManager = LinearLayoutManager(context)
        loadStaff()
    }

    private fun loadStaff() {
        db.collection("users")
            .whereEqualTo("role", "STAFF")
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val staff = snapshot?.toObjects(User::class.java) ?: emptyList()
                
                if (staff.isEmpty()) {
                    binding.tvEmptyStaff.visibility = View.VISIBLE
                    binding.rvStaffList.visibility = View.GONE
                } else {
                    binding.tvEmptyStaff.visibility = View.GONE
                    binding.rvStaffList.visibility = View.VISIBLE
                    // Simple adapter for now
                    val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                            return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {}
                        }
                        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                            val person = staff[position]
                            (holder.itemView.findViewById<View>(android.R.id.text1) as android.widget.TextView).text = person.name
                            (holder.itemView.findViewById<View>(android.R.id.text2) as android.widget.TextView).text = person.email
                        }
                        override fun getItemCount() = staff.size
                    }
                    binding.rvStaffList.adapter = adapter
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
