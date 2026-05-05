package com.hostelops.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.hostelops.R
import com.hostelops.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val args: LoginFragmentArgs by navArgs()
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvLoginTitle.text = "Login as ${args.role}"

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // FR-02: Institutional email validation for students
            if (args.role == "STUDENT" && !email.endsWith("@institution.edu")) {
                Toast.makeText(context, "Use institutional email only", Toast.LENGTH_SHORT).show()
                // return@setOnClickListener // Uncomment for real enforcement
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    checkUserRoleAndNavigate(it.user?.uid ?: "")
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        // Save FCM Token on login
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            db.collection("users").document(uid).update("fcmToken", token)
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role") ?: args.role
                when (role) {
                    "STUDENT" -> findNavController().navigate(R.id.action_loginFragment_to_studentDashboardFragment)
                    "STAFF" -> findNavController().navigate(R.id.action_loginFragment_to_staffDashboardFragment)
                    "ADMIN" -> findNavController().navigate(R.id.action_loginFragment_to_adminDashboardFragment)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
