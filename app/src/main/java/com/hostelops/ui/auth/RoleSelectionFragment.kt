package com.hostelops.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hostelops.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun navigateToLogin(role: String) {
        val action = RoleSelectionFragmentDirections.actionRoleSelectionFragmentToLoginFragment(role)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
