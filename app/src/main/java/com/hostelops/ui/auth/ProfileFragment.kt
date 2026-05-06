package com.hostelops.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.R
import com.hostelops.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()

        binding.btnUpdateAddress.setOnClickListener {
            updateAddress()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_global_roleSelectionFragment)
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        
        // Show Google photo if available
        user.photoUrl?.let {
            binding.ivProfileDetail.load(it) {
                crossfade(true)
                placeholder(R.drawable.ic_profile_placeholder)
                transformations(CircleCropTransformation())
            }
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.tvUserName.text = document.getString("name") ?: user.displayName ?: "User"
                    binding.tvUserEmail.text = document.getString("email") ?: user.email
                    binding.tvUserRole.text = document.getString("role")
                    binding.tvRollNo.text = document.getString("rollNo") ?: "N/A"
                    binding.tvPhone.text = document.getString("phone") ?: user.phoneNumber ?: "N/A"
                    binding.etProfileAddress.setText(document.getString("address") ?: "")
                    
                    if (document.getString("role") != "STUDENT") {
                        binding.tvRollNoLabel.text = "Employee ID"
                    }
                }
            }
    }

    private fun updateAddress() {
        val uid = auth.currentUser?.uid ?: return
        val newAddress = binding.etProfileAddress.text.toString().trim()

        binding.btnUpdateAddress.isEnabled = false
        db.collection("users").document(uid).update("address", newAddress)
            .addOnSuccessListener {
                binding.btnUpdateAddress.isEnabled = true
                Toast.makeText(context, "Address updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.btnUpdateAddress.isEnabled = true
                Toast.makeText(context, "Failed to update address", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
