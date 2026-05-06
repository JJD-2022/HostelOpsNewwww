package com.hostelops.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.R
import com.hostelops.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkCurrentUser()

        binding.cardStudent.setOnClickListener {
            navigateToLogin("STUDENT")
        }

        binding.cardStaff.setOnClickListener {
            navigateToLogin("STAFF")
        }

        binding.cardAdmin.setOnClickListener {
            navigateToLogin("ADMIN")
        }
    }

    private fun checkCurrentUser() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role") ?: return@addOnSuccessListener
                        navigateToDashboard(role)
                    }
                }
        }
    }

    private fun navigateToDashboard(role: String) {
        when (role) {
            "STUDENT" -> findNavController().navigate(R.id.action_roleSelectionFragment_to_studentDashboardFragment)
            "STAFF" -> findNavController().navigate(R.id.action_roleSelectionFragment_to_staffDashboardFragment)
            "ADMIN" -> findNavController().navigate(R.id.action_roleSelectionFragment_to_adminDashboardFragment)
        }
    }

    private fun navigateToLogin(role: String) {
        val action = RoleSelectionFragmentDirections.actionRoleSelectionFragmentToLoginFragment(role)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
