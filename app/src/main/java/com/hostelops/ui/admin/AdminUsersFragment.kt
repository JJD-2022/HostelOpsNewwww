package com.hostelops.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.databinding.FragmentAdminUsersBinding
import com.hostelops.models.User

class AdminUsersFragment : Fragment() {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: UserAdapter
    private var allUsers = listOf<User>()
    private var currentRole = "STUDENT"
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        setupSearch()
        loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(emptyList(), 
            onDeleteClick = { user -> showDeleteConfirmation(user) },
            onUserClick = { user -> showUserDetails(user) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(context)
        binding.rvUsers.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayoutUsers.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentRole = if (tab?.position == 0) "STUDENT" else "STAFF"
                binding.etUserSearch.setText("") // Clear search on tab change
                filterAndDisplay()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.etUserSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().lowercase()
                filterAndDisplay()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadUsers() {
        db.collection("users").addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            allUsers = value?.toObjects(User::class.java) ?: emptyList()
            filterAndDisplay()
        }
    }

    private fun filterAndDisplay() {
        val filtered = allUsers.filter { user ->
            user.role == currentRole && (
                user.name.lowercase().contains(searchQuery) ||
                user.email.lowercase().contains(searchQuery) ||
                user.rollNo.lowercase().contains(searchQuery)
            )
        }
        adapter.updateList(filtered)
        
        val roleName = if (currentRole == "STUDENT") "Student" else "Staff"
        binding.tvUserCount.text = "Total ${roleName}s: ${filtered.size}"
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("users").document(user.uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserDetails(user: User) {
        val details = """
            Name: ${user.name}
            Email: ${user.email}
            Role: ${user.role}
            Phone: ${user.phone}
            Roll No: ${user.rollNo}
            Address: ${user.address}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("User Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
