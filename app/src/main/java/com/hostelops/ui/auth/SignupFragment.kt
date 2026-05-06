package com.hostelops.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.R
import com.hostelops.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val args: SignupFragmentArgs by navArgs()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(context, "Google signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSignupTitle.text = "Signup as ${args.role}"
        
        // Adjust labels based on role
        if (args.role != "STUDENT") {
            binding.tilRollNo.hint = "Employee ID"
        }

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val rollNo = binding.etRollNo.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Name, Email and Password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val user = it.user
                    if (user != null) {
                        saveUserToFirestore(
                            uid = user.uid,
                            email = email,
                            role = args.role,
                            name = name,
                            rollNo = rollNo,
                            phone = phone,
                            address = address
                        )
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Signup Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.tvLoginLink.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnGoogleSignup.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val user = it.user
                if (user != null) {
                    // Pre-fill from Google account
                    saveUserToFirestore(
                        uid = user.uid,
                        email = user.email ?: "",
                        role = args.role,
                        name = user.displayName ?: "",
                        rollNo = "",
                        phone = user.phoneNumber ?: "",
                        address = ""
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Firebase auth with Google failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserToFirestore(
        uid: String, 
        email: String, 
        role: String,
        name: String,
        rollNo: String,
        phone: String,
        address: String
    ) {
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to email,
            "role" to role,
            "name" to name,
            "rollNo" to rollNo,
            "phone" to phone,
            "address" to address
        )

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Signup Successful", Toast.LENGTH_SHORT).show()
                navigateToDashboard(role)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error saving user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToDashboard(role: String) {
        when (role) {
            "STUDENT" -> findNavController().navigate(R.id.action_signupFragment_to_studentDashboardFragment)
            "STAFF" -> findNavController().navigate(R.id.action_signupFragment_to_staffDashboardFragment)
            "ADMIN" -> findNavController().navigate(R.id.action_signupFragment_to_adminDashboardFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
