package com.hostelops.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hostelops.databinding.FragmentNotificationsBinding
import com.hostelops.models.Notification

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvNotifications.layoutManager = LinearLayoutManager(context)
        loadNotifications()
    }

    private fun loadNotifications() {
        val user = auth.currentUser ?: return
        
        // Listen to notifications collection directly
        db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val allNotifications = value?.toObjects(Notification::class.java) ?: emptyList()
                
                // Fetch user role to filter
                db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                    val role = doc.getString("role")
                    
                    val filtered = allNotifications.filter { notif ->
                        notif.targetUid == user.uid || (notif.targetRole != null && notif.targetRole == role)
                    }

                    if (filtered.isEmpty()) {
                        binding.tvEmptyNotifications.visibility = View.VISIBLE
                        binding.rvNotifications.visibility = View.GONE
                    } else {
                        binding.tvEmptyNotifications.visibility = View.GONE
                        binding.rvNotifications.visibility = View.VISIBLE
                        binding.rvNotifications.adapter = NotificationAdapter(filtered)
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
