package com.hostelops.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
        binding.tvLoginLink.text = "Already a ${args.role.lowercase()}? Login here"
        
        if (args.role != "STUDENT") {
            binding.tilRollNo.hint = "Employee ID"
            binding.layoutStudentFields.visibility = View.GONE
        } else {
            binding.layoutStudentFields.visibility = View.VISIBLE
            val blocks = arrayOf("N Block", "Q Block", "R Block", "S Block", "T Block")
            binding.spinnerBlock.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, blocks))
        }

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            var rollNo = binding.etRollNo.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val block = binding.spinnerBlock.text.toString()
            val roomNo = binding.etRoomNo.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Name, Email and Password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Strict Institutional Email Enforcement
            if (!email.endsWith("@psgtech.ac.in")) {
                Toast.makeText(context, "Only @psgtech.ac.in emails are allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Auto-extract roll number if empty for students
            if (args.role == "STUDENT" && rollNo.isEmpty()) {
                rollNo = email.substringBefore("@")
            }

            if (args.role == "STUDENT" && (block.isEmpty() || roomNo.isEmpty())) {
                Toast.makeText(context, "Block and Room No are required for students", Toast.LENGTH_SHORT).show()
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
                            address = address,
                            block = block,
                            roomNo = roomNo
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
                val email = user?.email ?: ""
                
                if (!email.endsWith("@psgtech.ac.in")) {
                    auth.signOut()
                    Toast.makeText(context, "Only @psgtech.ac.in emails are allowed", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (user != null) {
                    val extractedRollNo = if (args.role == "STUDENT") email.substringBefore("@") else ""
                    saveUserToFirestore(
                        uid = user.uid,
                        email = user.email ?: "",
                        role = args.role,
                        name = user.displayName ?: "",
                        rollNo = extractedRollNo,
                        phone = user.phoneNumber ?: "",
                        address = "",
                        block = "",
                        roomNo = ""
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
        address: String,
        block: String,
        roomNo: String
    ) {
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to email,
            "role" to role,
            "name" to name,
            "rollNo" to rollNo,
            "phone" to phone,
            "address" to address,
            "block" to block,
            "roomNo" to roomNo
        )

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                if (phone.isEmpty() && role == "STUDENT") {
                    showPhonePromptDialog(role)
                } else {
                    Toast.makeText(context, "Signup Successful", Toast.LENGTH_SHORT).show()
                    navigateToDashboard(role)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error saving user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPhonePromptDialog(role: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Add Phone Number")
            .setMessage("Please add your phone number to help us contact you regarding your complaints.")
            .setPositiveButton("Go to Profile") { _, _ ->
                navigateToDashboard(role)
                findNavController().navigate(R.id.profileFragment)
            }
            .setNegativeButton("Later") { _, _ ->
                navigateToDashboard(role)
            }
            .setCancelable(false)
            .show()
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
