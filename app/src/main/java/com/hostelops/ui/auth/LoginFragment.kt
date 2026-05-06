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
import com.google.firebase.messaging.FirebaseMessaging
import com.hostelops.R
import com.hostelops.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val args: LoginFragmentArgs by navArgs()
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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

        binding.tvLoginTitle.text = "Login as ${args.role}"

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    checkUserRoleAndNavigate(it.user?.uid ?: "")
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.tvSignupLink.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragment_to_signupFragment(args.role)
            findNavController().navigate(action)
        }

        binding.btnGoogleLogin.setOnClickListener {
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
                    checkUserRoleAndNavigate(user.uid)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Firebase auth with Google failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        // Save FCM Token on login
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            db.collection("users").document(uid).update("fcmToken", token)
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: args.role
                    navigateToDashboard(role)
                } else {
                    // New user from Google Login - save them with the selected role
                    val user = auth.currentUser
                    val userMap = hashMapOf(
                        "uid" to uid,
                        "email" to (user?.email ?: ""),
                        "role" to args.role,
                        "name" to (user?.displayName ?: ""),
                        "phone" to (user?.phoneNumber ?: ""),
                        "rollNo" to "",
                        "address" to ""
                    )
                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            navigateToDashboard(args.role)
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToDashboard(role: String) {
        when (role) {
            "STUDENT" -> findNavController().navigate(R.id.action_loginFragment_to_studentDashboardFragment)
            "STAFF" -> findNavController().navigate(R.id.action_loginFragment_to_staffDashboardFragment)
            "ADMIN" -> findNavController().navigate(R.id.action_loginFragment_to_adminDashboardFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
